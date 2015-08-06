//	Copyright 2013, by the California Institute of Technology.
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
package gov.nasa.pds.imaging.generate;

import gov.nasa.pds.imaging.generate.cli.options.Flag;
import gov.nasa.pds.imaging.generate.cli.options.InvalidOptionException;
import gov.nasa.pds.imaging.generate.label.PDS3Label;
import gov.nasa.pds.imaging.generate.label.PDSObject;
import gov.nasa.pds.imaging.generate.util.Debugger;
import gov.nasa.pds.imaging.generate.util.ToolInfo;
import gov.nasa.pds.imaging.generate.util.Utility;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

/**
 * Class used as Command-line interface endpoint. Parses command-line arguments
 * and sends workflow onto the Generator object.
 * 
 * @author jpadams
 * 
 */
public class GenerateLauncher {

	/** Logger. **/
	private static Logger log = Logger.getLogger(GenerateLauncher.class.getName());

    private String basePath;
    private List<String> lblList;
    private File templateFile;
    private File outputPath;
    private boolean stdOut;

    private List<Generator> generatorList;

    public GenerateLauncher() {
        this.basePath = "";
        this.generatorList = new ArrayList<Generator>();
        this.lblList = new ArrayList<String>();
        this.outputPath = null;
        this.stdOut = false;
        this.templateFile = null;
    }

    /**
     * Displays tool usage.
     * 
     */
    public final void displayHelp() {
        final int maxWidth = 80;
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(maxWidth, "generate <options>",
                null, Flag.getOptions(), null);
    }

    /**
     * Displays the current version and disclaimer notice.
     * 
     */
    public final void displayVersion() {
        System.err.println("\n" + ToolInfo.getName());
        System.err.println(ToolInfo.getVersion());
        System.err.println("Release Date: " + ToolInfo.getReleaseDate());
        System.err.println(ToolInfo.getCopyright() + "\n");
    }

    public final void generate() throws Exception {
    	for (Generator generator : this.generatorList) {
    		generator.generate(this.stdOut);
    	}
    }

    private String getConfigPath() {
        return (new File(System.getProperty("java.class.path"))).getParentFile().getParent() + "/conf";
    }

    /**
     * A method to parse the command-line arguments.
     * 
     * @param args
     *            The command-line arguments
     * @return A class representation of the command-line arguments
     * 
     * @throws ParseException
     *             If there was an error during parsing.
     */
    public final CommandLine parse(final String[] args) throws ParseException {
        final CommandLineParser parser = new GnuParser();
        return parser.parse(Flag.getOptions(), args);
    }

    /**
     * Examines the command-line arguments passed into the Harvest Tool and
     * takes the appropriate action based on what flags were set.
     * 
     * @param line
     *            A class representation of the command-line arguments.
     * 
     * @throws Exception
     *             If there was an error while querying the options that were
     *             set on the command-line.
     */    
    public final void query(final CommandLine line) throws Exception {
        final List<Option> processedOptions = Arrays.asList(line.getOptions());
        for (final Option o : processedOptions) {
            if (o.getOpt().equals(Flag.HELP.getShortName())) {
                displayHelp();
                System.exit(0);
            } else if (o.getOpt().equals(Flag.VERSION.getShortName())) {
                displayVersion();
                System.exit(0);
            } else if (o.getOpt().equals(Flag.PDS3.getShortName())) {
            	this.lblList = new ArrayList<String>();
            	for (String path : (List<String>) o.getValuesList()) {
            		Debugger.debug(Utility.getAbsolutePath(path));
            		this.lblList.add(Utility.getAbsolutePath(path));
            	}
            } else if (o.getOpt().equals(Flag.TEMPLATE.getShortName())) {
                this.templateFile = new File(Utility.getAbsolutePath(o.getValue().trim()));
            } else if (o.getOpt().equals(Flag.OUTPUT.getShortName())) {
                this.outputPath = new File(Utility.getAbsolutePath(o.getValue().trim()));
            } else if (o.getOpt().equals(Flag.BASEPATH.getShortName())) {
                this.basePath = o.getValue().trim();
            }
        }
        
        // First check we have a Template File
        if (this.templateFile == null) { // Throw error if no template file
                                         // specified
            throw new InvalidOptionException("Missing -t flag.  Template file must be specified.");
        }
        
        // Now let's check where the output is going
        if (this.outputPath != null && !this.outputPath.isDirectory()) {
        	if (this.outputPath.isFile()) {
	        	throw new InvalidOptionException("Output path is invalid. " + 
	        			"Must be existing directory or new path.");
        	} else {
        		this.outputPath.mkdirs();
        	}
        }

        // Let's default to the one label if -p flag was specified,
        // otherwise loop through the lbl list
        if (this.lblList == null) {
        	Debugger.debug("in here");
    		throw new InvalidOptionException("Missing -p or -l flags.  " + 
                    "One or many PDS3 label must be specified.");
        } else {
        	String filepath;
        	PDSObject pdsObj;
        	File outputFile;
        	for (String lbl : this.lblList) {
        		// Make sure the lbl exists
        		if ((new File(lbl)).isFile()) {
        			// Set the pds3 lable object
        			pdsObj = new PDS3Label(lbl);
	        		pdsObj.setMappings();
	        		
	        		// Let's get the output file ready
	        		// Build up the output filepath
	        		outputFile = new File(lbl);
	        		filepath = outputFile.getParent();
	        		filepath = filepath + "/" + 
	        				outputFile.getName().split("\\.")[0] + ".xml";
	        		if (this.outputPath != null) {
	        			filepath = this.outputPath.getAbsolutePath() + "/" + 
	        						filepath.replace(this.basePath, "");
	        		}
	        		outputFile = new File(filepath);
	        		
	        		Debugger.debug(outputFile.getAbsolutePath());
			        this.generatorList.add(new Generator(pdsObj, this.templateFile,
			                outputFile));
        		} else {
        			log.warning(lbl + " does not exist.");
        		}
        	}
        }

    }
    
    /**
     * @param args
     */
    public static void main(final String[] args) {
        if (args.length == 0) {
            System.out.println("\nType 'generate -h' for usage");
            System.exit(0);
        }
        try {
            final GenerateLauncher launcher = new GenerateLauncher();
            final CommandLine commandline = launcher.parse(args);
            launcher.query(commandline);
            launcher.generate();
            // launcher.closeHandlers();
        } catch (final ParseException pEx) {
            System.err.println("Command-line parse failure: "
                    + pEx.getMessage());
            System.exit(1);
        } catch (final Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }    

}
