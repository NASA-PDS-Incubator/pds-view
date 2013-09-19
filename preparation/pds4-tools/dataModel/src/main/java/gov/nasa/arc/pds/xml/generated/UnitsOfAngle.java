//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.06.06 at 03:37:38 PM PDT 
//


package gov.nasa.arc.pds.xml.generated;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Units_of_Angle.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="Units_of_Angle">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="arcmin"/>
 *     &lt;enumeration value="arcsec"/>
 *     &lt;enumeration value="deg"/>
 *     &lt;enumeration value="hr"/>
 *     &lt;enumeration value="mrad"/>
 *     &lt;enumeration value="rad"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "Units_of_Angle")
@XmlEnum
public enum UnitsOfAngle {

    @XmlEnumValue("arcmin")
    ARCMIN("arcmin"),
    @XmlEnumValue("arcsec")
    ARCSEC("arcsec"),
    @XmlEnumValue("deg")
    DEG("deg"),
    @XmlEnumValue("hr")
    HR("hr"),
    @XmlEnumValue("mrad")
    MRAD("mrad"),
    @XmlEnumValue("rad")
    RAD("rad");
    private final String value;

    UnitsOfAngle(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static UnitsOfAngle fromValue(String v) {
        for (UnitsOfAngle c: UnitsOfAngle.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}