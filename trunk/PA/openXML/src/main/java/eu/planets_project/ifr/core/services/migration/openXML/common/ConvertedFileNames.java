/*
 * ConvertedFileNames.java
 *
 * Created on 29 June 2007, 10:44
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package eu.planets_project.ifr.core.services.migration.openXML.common;

/**
 *
 * @author CFwilson
 */
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.3-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2007.06.28 at 02:39:04 PM BST 
//


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.planets-project.eu/xml/ns/planets/services/odfInfo}input"/>
 *         &lt;element ref="{http://www.planets-project.eu/xml/ns/planets/services/odfInfo}output"/>
 *         &lt;element ref="{http://www.planets-project.eu/xml/ns/planets/services/odfInfo}actual"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "input",
    "output",
    "actual"
})
@XmlRootElement(name = "names")
public class ConvertedFileNames {

    @XmlElement(required = true, name="input")
    protected String input;
    @XmlElement(required = true, name="output")
    protected String output;
    @XmlElement(required = true, name="actual")
    protected String actual;

    public ConvertedFileNames()
    {
        this.input = new String("");
        this.output = new String("");
        this.actual = new String("");
    }
    /*
     * No default constructor, needs at least two names
     */
    public ConvertedFileNames(String inputVal, String outputVal, String actualVal) {
        this.input = new String(inputVal);
        this.output = new String(outputVal);
        this.actual = new String(actualVal);
    }

    public ConvertedFileNames(String inputVal, String outputVal) {
        this.input = new String(inputVal);
        this.output = new String(outputVal);
        this.actual = new String(outputVal);
    }
    
    /**
     * Gets the value of the input property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInput() {
        return input;
    }

    /**
     * Sets the value of the input property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInput(String inputVal) {
        this.input = inputVal;
    }

    /**
     * Gets the value of the output property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOutput() {
        return output;
    }

    /**
     * Sets the value of the output property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOutput(String outputVal) {
        this.output = outputVal;
    }

    /**
     * Gets the value of the actual property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getActual() {
        return actual;
    }

    /**
     * Sets the value of the actual property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setActual(String actualVal) {
        this.actual = actualVal;
    }

}