//	Copyright 2009-2014, by the California Institute of Technology.
//	ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
//	Any commercial use must be negotiated with the Office of Technology
//	Transfer at the California Institute of Technology.
//
//	This software is subject to U. S. export control laws and regulations
//	(22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the extent that the software
//	is subject to U.S. export control laws and regulations, the recipient has
//	the responsibility to obtain export licenses or other export authority as
//	may be required before exporting such information to foreign countries or
//	providing access to foreign nationals.
//
//	$Id$
//

package gov.nasa.pds.tools.label;

import gov.nasa.pds.tools.label.validate.DefaultDocumentValidator;
import gov.nasa.pds.tools.label.validate.DocumentValidator;
import gov.nasa.pds.tools.label.validate.ExternalValidator;
import gov.nasa.pds.tools.util.Utility;
import gov.nasa.pds.tools.util.VersionInfo;
import gov.nasa.pds.tools.util.XMLErrorListener;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.Configuration;
import net.sf.saxon.event.ParseOptions;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.xpath.XPathEvaluator;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class is responsible for providing utility functions for validating PDS
 * XML Labels.
 *
 * @author pramirez
 *
 */
public class LabelValidator {
  private Map<String, Boolean> configurations = new HashMap<String, Boolean>();
  private List<URL> userSchemaFiles;
  private List<URL> userSchematronFiles;
  private List<Transformer> userSchematronTransformers;
  private String modelVersion;
  private DocumentBuilder cachedValidator;
  private List<Transformer> cachedSchematron;
  private XMLCatalogResolver resolver;
  private Boolean useLabelSchema;
  private Boolean useLabelSchematron;
  private Map<String, Transformer> cachedLabelSchematrons;

  public static final String SCHEMA_CHECK = "gov.nasa.pds.tools.label.SchemaCheck";
  public static final String SCHEMATRON_CHECK = "gov.nasa.pds.tools.label.SchematronCheck";

  private List<ExternalValidator> externalValidators;
  private List<DocumentValidator> documentValidators;
  private CachedEntityResolver cachedEntityResolver;
  private DocumentBuilderFactory docBuilderFactory;
  private SchemaFactory schemaFactory;
  private Schema validatingSchema;
  private SchematronTransformer schematronTransformer;

  /**
   * Default constructor.
   *
   * @throws ParserConfigurationException If there was an error setting up
   * the configuration of the parser that is reposnible for doing the
   * label validation.
   *
   * @throws TransformerConfigurationException If there was an error setting
   * up the Transformer responsible for doing the transformations of the
   * schematrons.
   */
  public LabelValidator() throws ParserConfigurationException,
  TransformerConfigurationException {
    this.configurations.put(SCHEMA_CHECK, true);
    this.configurations.put(SCHEMATRON_CHECK, true);
    modelVersion = VersionInfo.getDefaultModelVersion();
    cachedValidator = null;
    cachedSchematron = new ArrayList<Transformer>();
    userSchemaFiles = null;
    userSchematronFiles = null;
    userSchematronTransformers = new ArrayList<Transformer>();
    resolver = null;
    externalValidators = new ArrayList<ExternalValidator>();
    documentValidators = new ArrayList<DocumentValidator>();
    useLabelSchema = false;
    useLabelSchematron = false;
    cachedLabelSchematrons = new HashMap<String, Transformer>();
    cachedEntityResolver = new CachedEntityResolver();
    validatingSchema = null;
    // Support for XSD 1.1
    schemaFactory = SchemaFactory
        .newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
    docBuilderFactory = DocumentBuilderFactory.newInstance();
    docBuilderFactory.setNamespaceAware(true);
    docBuilderFactory.setXIncludeAware(true);
    //TODO: Do we want to omit the xml:base attribute from the merged xml?
    docBuilderFactory.setFeature(
        "http://apache.org/xml/features/xinclude/fixup-base-uris",
        false);

    documentValidators.add(new DefaultDocumentValidator());
    schematronTransformer = new SchematronTransformer();
  }

  /**
   * Pass in a list of schemas to validate against.
   *
   * @param schemaFiles A list of schema URLs.
   *
   */
  public void setSchema(List<URL> schemaFiles) {
    userSchemaFiles = schemaFiles;
  }

  /**
   * Pass in a list of transformed schematrons to validate
   * against.
   *
   * @param schematrons A list of transformed schematrons.
   */
  public void setSchematrons(List<Transformer> schematrons) {
    userSchematronTransformers = schematrons;
  }

  /**
   * Pass in a hash map of schematron URLs to its transformed
   * schematron object. This is used when validating a label
   * against it's referenced schematron.
   *
   * @param schematronMap
   */
  public void setLabelSchematrons(
      Map<String, Transformer> schematronMap) {
    cachedLabelSchematrons = schematronMap;
  }

  /**
   * Pass in a list of schematron files to validate against.
   *
   * @param schematronFiles A list of schematron URLs.
   */
  public void setSchematronFiles(List<URL> schematronFiles) {
    userSchematronFiles = schematronFiles;
  }

  /**
   * Pass in a list of Catalog files to use during the validation
   * step.
   *
   * @param catalogFiles
   */
  public void setCatalogs(String[] catalogFiles) {
    resolver = new XMLCatalogResolver();
    resolver.setPreferPublic(true);
    resolver.setCatalogList(catalogFiles);
  }

  private List<StreamSource> loadSchemaSources(List<URL> schemas)
      throws IOException {
    List<StreamSource> sources = new ArrayList<StreamSource>();
    for (URL schema : schemas) {
      InputStream in = null;
      URLConnection conn = null;
      try {
        conn = schema.openConnection();
        in = Utility.openConnection(conn);
        InputSource inputSource = new InputSource(
            new ByteArrayInputStream(IOUtils.toByteArray(in)));
        inputSource.setSystemId(schema.toString());
        StreamSource streamSource = new StreamSource(
            inputSource.getByteStream());
        streamSource.setSystemId(schema.toString());
        sources.add(streamSource);
      } finally {
        IOUtils.closeQuietly(in);
        IOUtils.close(conn);
      }
    }
    return sources;

  }

  private List<StreamSource> loadSchemaSources(String[] schemaFiles) {
    List<StreamSource> sources = new ArrayList<StreamSource>();
    for (String schemaFile : schemaFiles) {
      sources.add(new StreamSource(schemaFile));
    }
    return sources;
  }

  private List<StreamSource> loadSchemaSourcesFromJar() {
    String[] schemaFiles = VersionInfo.getSchemasFromJar(modelVersion).toArray(
        new String[0]);
    List<StreamSource> sources = new ArrayList<StreamSource>();
    for (String schemaFile : schemaFiles) {
      sources.add(new StreamSource(LabelValidator.class
          .getResourceAsStream(VersionInfo.getSchemaRefFromJar(modelVersion,
              schemaFile))));
    }
    return sources;
  }

  public synchronized void validate(ExceptionContainer container, File labelFile)
  throws SAXException, IOException, ParserConfigurationException,
  TransformerException, MissingLabelSchemaException {
    validate(container, labelFile.toURI().toURL());
  }

  /**
   * Validates the label against schema and schematron constraints.
   *
   * @param container
   *          to store output messages in
   * @param url
   *          label to validate
   *
   * @throws SAXException
   * @throws IOException
   * @throws ParserConfigurationException
   * @throws TransformerException
   * @throws MissingLabelSchemaException
   */
  public synchronized void validate(ExceptionContainer container, URL url)
      throws SAXException, IOException, ParserConfigurationException,
      TransformerException, MissingLabelSchemaException {
    List<String> labelSchematronRefs = new ArrayList<String>();
    Document xml = null;
    // Are we perfoming schema validation?
    if (performsSchemaValidation()) {
      // Do we have a schema we have loaded previously?
      if (cachedValidator == null) {
        // If catalog is used allow resources to be loaded for schemas
        if (resolver != null) {
          schemaFactory.setProperty(
              "http://apache.org/xml/properties/internal/entity-resolver",
              resolver);
        }
        // Allow errors that happen in the schema to be logged there
        if (container != null) {
          schemaFactory.setErrorHandler(new LabelErrorHandler(container));
        }
        // Time to load schema that will be used for validation
        if (userSchemaFiles != null) {
          // User has specified schema files to use
          validatingSchema = schemaFactory.newSchema(loadSchemaSources(
              userSchemaFiles).toArray(new StreamSource[0]));
        } else if (resolver == null) {
          if (useLabelSchema) {
            validatingSchema = schemaFactory.newSchema();
          } else if (VersionInfo.isInternalMode()) {
            // There is no catalog file

            // No external schema directory was specified so load from jar
            validatingSchema = schemaFactory
                .newSchema(loadSchemaSourcesFromJar().toArray(
                    new StreamSource[0]));
          } else {
            // Load from user specified external directory
            validatingSchema = schemaFactory.newSchema(loadSchemaSources(
                VersionInfo.getSchemasFromDirectory().toArray(new String[0]))
                .toArray(new StreamSource[0]));
          }
        } else {
          // We're only going to use the catalog to validate against.
          validatingSchema = schemaFactory.newSchema();
        }
        // Time to create a validator from our schema
        docBuilderFactory.setSchema(validatingSchema);

        cachedValidator = docBuilderFactory.newDocumentBuilder();
        // Allow access to the catalog from the parser
        if (resolver != null) {
          cachedValidator.setEntityResolver(resolver);
        } else if (useLabelSchema) {
          cachedValidator.setEntityResolver(cachedEntityResolver);
        }
      } else {
        // Create a new instance of the DocumentBuilder if validating
        // against a label's schema.
        if (useLabelSchema) {
          cachedValidator = docBuilderFactory.newDocumentBuilder();
          cachedValidator.setEntityResolver(cachedEntityResolver);
        }
      }
      // Capture messages in a container
      if (container != null) {
        cachedValidator.setErrorHandler(new LabelErrorHandler(container));
      }
      // Finally validate the file
      xml = cachedValidator.parse(url.openStream(), url.toString());

      // If validating against the label supplied schema, check
      // if the xsi:schemalocation attribute was defined in the label.
      // If it is not found, throw an exception.
      if (useLabelSchema) {
        Element root = xml.getDocumentElement();
        if (!root.hasAttribute("xsi:schemaLocation")) {
          throw new MissingLabelSchemaException(
              "No schema(s) specified in the label.");
        }
      }
    } else {
      // No Schema validation will be performed. Just parse the label
      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
      // Capture messages in a container
      if (container != null) {
        docBuilder.setErrorHandler(new LabelErrorHandler(container));
      }
      if (resolver != null) {
        docBuilder.setEntityResolver(resolver);
      } else if (useLabelSchema) {
        docBuilder.setEntityResolver(cachedEntityResolver);
      }
      xml = docBuilder.parse(url.openStream(), url.toString());
    }

    // Validate with any schematron files we have
    if (performsSchematronValidation()) {
      // Look for schematron files specified in a label
      if (useLabelSchematron) {
        labelSchematronRefs = getSchematrons(xml.getChildNodes(), url,
            container);
      }
      if (cachedSchematron.isEmpty()) {
        if (useLabelSchematron) {
          cachedSchematron = loadLabelSchematrons(labelSchematronRefs, url,
              container);
        } else if ( (userSchematronTransformers.isEmpty()) &&
            (userSchematronFiles == null) ) {
          // If user does not provide schematron then use ones in jar if available
          for (String schematronFile : VersionInfo
              .getSchematronsFromJar(modelVersion)) {
            cachedSchematron.add(schematronTransformer.transform(
                new StreamSource(LabelValidator.class.getResourceAsStream(
                    VersionInfo.getSchematronRefFromJar(
                        modelVersion, schematronFile)))
                ));
          }
        } else {
          if (!userSchematronTransformers.isEmpty()) {
            cachedSchematron = userSchematronTransformers;
          } else if (userSchematronFiles != null) {
            List<Transformer> transformers = new ArrayList<Transformer>();
            for (URL schematron : userSchematronFiles) {
              StreamSource source = new StreamSource(schematron.toString());
              source.setSystemId(schematron.toString());
              Transformer transformer = schematronTransformer.transform(
                  source, container);
              transformers.add(transformer);
            }
            cachedSchematron = transformers;
          }
        }
      } else {
        // If there are cached schematrons....
        if (useLabelSchematron) {
          if (!userSchematronTransformers.isEmpty()) {
            cachedSchematron = userSchematronTransformers;
          } else {
            cachedSchematron = loadLabelSchematrons(labelSchematronRefs, url,
              container);
          }
        }
      }
      // Boiler plate to handle parsing report outputs from schematron
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      DocumentBuilder parser = dbf.newDocumentBuilder();
      for (Transformer schematron : cachedSchematron) {
        StringWriter report = new StringWriter();
        // Apply the rules specified in the schematron file
        schematron.transform(new StreamSource(url.openStream()),
            new StreamResult(report));
        // Output is svrl:schematron-output document
        // Select out svrl:failed-assert nodes and put into exception container
        Document reportDoc = parser.parse(new InputSource(new StringReader(
            report.toString())));
        NodeList nodes = reportDoc.getElementsByTagNameNS(
            "http://purl.oclc.org/dsdl/svrl", "failed-assert");
        for (int i = 0; i < nodes.getLength(); i++) {
          Node node = nodes.item(i);
          // Add an error for each failed asssert
          container.addException(processFailedAssert(url, node));
        }
      }
    }
    if (!externalValidators.isEmpty()) {
      // Perform any other additional checks that were added
      for(ExternalValidator ev : externalValidators) {
        ev.validate(container, url);
      }
    }

    // Perform any additional checks that were added
    if (!documentValidators.isEmpty()) {
      SAXSource saxSource = new SAXSource(new InputSource(url.toString()));
      saxSource.setSystemId(url.toString());
      DocumentInfo docInfo = parse(saxSource);
      for (DocumentValidator dv : documentValidators) {
        dv.validate(container, docInfo);
      }
    }
  }

  public void validate(File labelFile) throws SAXException, IOException,
      ParserConfigurationException, TransformerException,
      MissingLabelSchemaException {
    validate(null, labelFile);
  }

  public List<String> getSchematrons(NodeList nodeList, URL url,
      ExceptionContainer container) {
    List<String> results = new ArrayList<String>();

    for (int i = 0; i < nodeList.getLength(); i++) {
      if (nodeList.item(i).getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
        ProcessingInstruction pi = (ProcessingInstruction) nodeList.item(i);
        if ("xml-model".equalsIgnoreCase(pi.getTarget())) {
          Pattern pattern = Pattern.compile(
              "href=\\\"([^=]*)\\\"( schematypens=\\\"([^=]*)\\\")?");
          String filteredData = pi.getData().replaceAll("\\s+", " ");
          Matcher matcher = pattern.matcher(filteredData);
          if (matcher.matches()) {
            String value = matcher.group(1).trim();
            URL schematronRef = null;
            try {
              schematronRef = new URL(value);
            } catch (MalformedURLException ue) {
              // The schematron specification value does not appear to be
              // a URL. Assume a local reference to the schematron and
              // attempt to resolve it.
              try {
                schematronRef = new URL(new URL(pi.getBaseURI()), value);
              } catch (MalformedURLException mue) {
                container.addException(new LabelException(ExceptionType.ERROR,
                    "Cannot resolve schematron specification '"
                        + value + "': " + mue.getMessage(),
                        url.toString()));
                continue;
              }
            }
            results.add(schematronRef.toString());
          }
        }
      }
    }
    return results;
  }

  private List<Transformer> loadLabelSchematrons(List<String> schematronSources,
      URL url, ExceptionContainer container) {
    List<Transformer> transformers = new ArrayList<Transformer>();
    for (String source : schematronSources) {
      try {
        Transformer transformer = cachedLabelSchematrons.get(source);
        if (transformer != null) {
          transformers.add(transformer);
        } else {
          URL sourceUrl = new URL(source);
          try {
            transformer = schematronTransformer.transform(sourceUrl);
            cachedLabelSchematrons.put(source, transformer);
          } catch (TransformerException te) {
            throw new Exception("Schematron '" + source + "' error: "
                + te.getMessage());
          }
        }
      } catch (Exception e) {
        String message = "Error occurred while loading schematron: "
            + e.getMessage();
        container.addException(new LabelException(ExceptionType.ERROR,
            message, url.toString()));
      }
    }
    return transformers;
  }

  /**
   * Process a failed assert message from the schematron report.
   *
   * @param url The url of the xml being validated.
   * @param node The node object containing the failed assert message.
   *
   * @return A LabelException object.
   */
  private LabelException processFailedAssert(URL url, Node node) {
    ExceptionType exceptionType = ExceptionType.ERROR;
    if (node.getAttributes().getNamedItem("role") != null) {
      String type = node.getAttributes().getNamedItem("role")
      .getTextContent();
      if ("warn".equalsIgnoreCase(type) ||
          "warning".equalsIgnoreCase(type)) {
        exceptionType = ExceptionType.WARNING;
      } else if ("info".equalsIgnoreCase(type)) {
        exceptionType = ExceptionType.INFO;
      }
    }
    return new LabelException(
        exceptionType,
        node.getTextContent().trim(),
        url.toString(),
        node.getAttributes().getNamedItem("location").getTextContent(),
        node.getAttributes().getNamedItem("test").getTextContent()
        );

  }

  private DocumentInfo parse(SAXSource source) throws TransformerException {
    XPathEvaluator xpath = new XPathEvaluator();
    Configuration configuration = xpath.getConfiguration();
    configuration.setLineNumbering(true);
    configuration.setXIncludeAware(true);
    ParseOptions options = new ParseOptions();
    options.setErrorListener(new XMLErrorListener());
    options.setLineNumbering(true);
    options.setXIncludeAware(true);
    return configuration.buildDocument(source, options);
  }

  public String getModelVersion() {
    return modelVersion;
  }

  public void setModelVersion(String modelVersion) throws ValidatorException {
    if (!VersionInfo.getSupportedModels().contains(modelVersion)) {
      throw new ValidatorException(ExceptionType.ERROR, "Unsupported model version \""
          + modelVersion + "\" use one of "
          + VersionInfo.getSupportedModels().toString());
    }
    this.modelVersion = modelVersion;
  }

  public Boolean performsSchemaValidation() {
    return getConfiguration(SCHEMA_CHECK);
  }

  public void setSchemaCheck(Boolean value) {
    setSchemaCheck(value, false);
  }

  public void setSchemaCheck(Boolean value, Boolean useLabelSchema) {
    this.setConfiguration(SCHEMA_CHECK, value);
    this.useLabelSchema = useLabelSchema;
  }

  public Boolean performsSchematronValidation() {
    return getConfiguration(SCHEMATRON_CHECK);
  }

  public void setSchematronCheck(Boolean value) {
    setSchematronCheck(value, false);
  }

  public void setSchematronCheck(Boolean value, Boolean useLabelSchematron) {
    this.setConfiguration(SCHEMATRON_CHECK, value);
    this.useLabelSchematron = useLabelSchematron;
  }

  public Boolean getConfiguration(String key) {
    return this.configurations.containsKey(key) ? this.configurations.get(key)
        : false;
  }

  public void setConfiguration(String key, Boolean value) {
    this.configurations.put(key, value);
  }

  public void addValidator(ExternalValidator validator) {
    this.externalValidators.add(validator);
  }

  public void addValidator(DocumentValidator validator) {
    this.documentValidators.add(validator);
  }

  public void setCachedEntityResolver(CachedEntityResolver resolver) {
    this.cachedEntityResolver = resolver;
  }

  public static void main(String[] args) throws Exception {
    LabelValidator lv = new LabelValidator();
    lv.setCatalogs(new String[]{args[1]});
    ExceptionContainer container = new ExceptionContainer();
    lv.validate(container, new File(args[0]));
    for (LabelException ex : container.getExceptions()) {
      System.out.println(ex.getMessage());
    }
  }
}
