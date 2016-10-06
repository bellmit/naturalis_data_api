//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.10.06 at 01:26:24 PM CEST 
//


package nl.naturalis.nba.dao.format.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for FieldXmlConfig complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="FieldXmlConfig">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;choice>
 *           &lt;element name="path" type="{http://data.naturalis.nl/nba-dataset-config}PathXmlConfig"/>
 *           &lt;element name="constant" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *           &lt;element name="calculator" type="{http://data.naturalis.nl/nba-dataset-config}PluginXmlConfig"/>
 *         &lt;/choice>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="term" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FieldXmlConfig", propOrder = {
    "path",
    "constant",
    "calculator"
})
public class FieldXmlConfig {

    protected PathXmlConfig path;
    protected String constant;
    protected PluginXmlConfig calculator;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "term")
    @XmlSchemaType(name = "anyURI")
    protected String term;

    /**
     * Gets the value of the path property.
     * 
     * @return
     *     possible object is
     *     {@link PathXmlConfig }
     *     
     */
    public PathXmlConfig getPath() {
        return path;
    }

    /**
     * Sets the value of the path property.
     * 
     * @param value
     *     allowed object is
     *     {@link PathXmlConfig }
     *     
     */
    public void setPath(PathXmlConfig value) {
        this.path = value;
    }

    /**
     * Gets the value of the constant property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getConstant() {
        return constant;
    }

    /**
     * Sets the value of the constant property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setConstant(String value) {
        this.constant = value;
    }

    /**
     * Gets the value of the calculator property.
     * 
     * @return
     *     possible object is
     *     {@link PluginXmlConfig }
     *     
     */
    public PluginXmlConfig getCalculator() {
        return calculator;
    }

    /**
     * Sets the value of the calculator property.
     * 
     * @param value
     *     allowed object is
     *     {@link PluginXmlConfig }
     *     
     */
    public void setCalculator(PluginXmlConfig value) {
        this.calculator = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the term property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTerm() {
        return term;
    }

    /**
     * Sets the value of the term property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTerm(String value) {
        this.term = value;
    }

}
