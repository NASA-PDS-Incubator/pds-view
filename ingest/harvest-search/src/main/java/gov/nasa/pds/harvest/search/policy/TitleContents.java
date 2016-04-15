// Copyright 2006-2013, by the California Institute of Technology.
// ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
// Any commercial use must be negotiated with the Office of Technology Transfer
// at the California Institute of Technology.
//
// This software is subject to U. S. export control laws and regulations
// (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the extent that the software
// is subject to U.S. export control laws and regulations, the recipient has
// the responsibility to obtain export licenses or other export authority as
// may be required before exporting such information to foreign countries or
// providing access to foreign nationals.
//
// $Id$

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2013.02.04 at 02:26:40 PM PST
//


package gov.nasa.pds.harvest.search.policy;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for TitleContents complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="TitleContents">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}elementName" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute ref="{}appendFilename"/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TitleContents", propOrder = {
    "elementName"
})
public class TitleContents {

    protected List<ElementName> elementName;
    @XmlAttribute
    protected Boolean appendFilename;

    /**
     * Gets the value of the elementName property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the elementName property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getElementName().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ElementName }
     *
     *
     */
    public List<ElementName> getElementName() {
        if (elementName == null) {
            elementName = new ArrayList<ElementName>();
        }
        return this.elementName;
    }

    /**
     * Gets the value of the appendFilename property.
     *
     * @return
     *     possible object is
     *     {@link Boolean }
     *
     */
    public boolean isAppendFilename() {
        if (appendFilename == null) {
            return false;
        } else {
            return appendFilename;
        }
    }

    /**
     * Sets the value of the appendFilename property.
     *
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *
     */
    public void setAppendFilename(Boolean value) {
        this.appendFilename = value;
    }

}
