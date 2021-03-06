/*
 * ConvertedFile.java
 *
 * Created on 29 June 2007, 10:32
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
import java.util.Date;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
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
 *         &lt;element ref="{http://www.planets-project.eu/xml/ns/planets/services/odfInfo}result"/>
 *         &lt;element ref="{http://www.planets-project.eu/xml/ns/planets/services/odfInfo}starttime"/>
 *         &lt;element ref="{http://www.planets-project.eu/xml/ns/planets/services/odfInfo}endtime"/>
 *         &lt;element ref="{http://www.planets-project.eu/xml/ns/planets/services/odfInfo}names"/>
 *         &lt;element ref="{http://www.planets-project.eu/xml/ns/planets/services/odfInfo}fileinfo"/>
 *       &lt;/sequence>
 *       &lt;attribute name="no" use="required" type="{http://www.w3.org/2001/XMLSchema}integer" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "conversionResult",
    "startTime",
    "endTime",
    "convertedFileNames",
    "convertedFileInfo"
})
@XmlRootElement(name = "file")
public class ConvertedFile {

    @XmlElement(required = true, name="result")
    protected ConversionResult conversionResult;
    @XmlElement(required = true, name="startTime")
    protected Date startTime = new Date();
    @XmlElement(required = true, name="endTime")
    protected Date endTime = new Date();
    @XmlElement(required = true, name="names")
    protected ConvertedFileNames convertedFileNames;
    @XmlElement(required = true, name="fileInfo")
    protected ConvertedFileInfo convertedFileInfo = new ConvertedFileInfo();
    @XmlAttribute(required = true, name="no")
    protected int number;

    protected ConvertedFile() {
    }
    
    
    /**
     * @param fileNumber
     * @param inputFileName
     * @param outputFileName
     */
    public ConvertedFile(int fileNumber, String inputFileName, String outputFileName) {
        conversionResult = new ConversionResult();
        convertedFileNames = new ConvertedFileNames(inputFileName, outputFileName);
        this.number = fileNumber;
    }
    /**
     * Gets the value of the result property.
     * 
     * @return
     *     possible object is
     *     {@link ConversionResult }
     *     
     */
    public ConversionResult getResult() {
        return conversionResult;
    }

    /**
     * Sets the value of the result property.
     * 
     * @param value
     *     allowed object is
     *     {@link ConversionResult }
     *     
     */
    public void setConversionResult(ConversionResult value) {
        this.conversionResult = value;
    }

    /**
     * Gets the value of the starttime property.
     * 
     * @return
     *     possible object is
     *     {@link javax.xml.datatype.XMLGregorianCalendar }
     *     
     */
    public Date getStarttime() {
        return startTime;
    }

    /**
     * Sets the value of the starttime property.
     * 
     * @param value
     *     allowed object is
     *     {@link javax.xml.datatype.XMLGregorianCalendar }
     *     
     */
    public void setStarttime(Date value) {
        this.startTime = value;
    }

    /**
     * Gets the value of the endtime property.
     * 
     * @return
     *     possible object is
     *     {@link javax.xml.datatype.XMLGregorianCalendar }
     *     
     */
    public Date getEndtime() {
        return endTime;
    }

    /**
     * Sets the value of the endtime property.
     * 
     * @param value
     *     allowed object is
     *     {@link javax.xml.datatype.XMLGregorianCalendar }
     *     
     */
    public void setEndtime(Date value) {
        this.endTime = value;
    }

    /**
     * Gets the value of the names property.
     * 
     * @return
     *     possible object is
     *     {@link ConvertedFileNames }
     *     
     */
    public ConvertedFileNames getConvertedFileNames() {
        return convertedFileNames;
    }

    /**
     * Sets the value of the names property.
     * 
     * @param value
     *     allowed object is
     *     {@link ConvertedFileNames }
     *     
     */
    public void setConvertedFileNames(ConvertedFileNames value) {
        this.convertedFileNames = value;
    }

    /**
     * Gets the value of the fileinfo property.
     * 
     * @return
     *     possible object is
     *     {@link ConvertedFileInfo }
     *     
     */
    public ConvertedFileInfo getConvertedFileInfo() {
        return convertedFileInfo;
    }

    /**
     * Sets the value of the fileinfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link ConvertedFileInfo }
     *     
     */
    public void setConvertedFileInfo(ConvertedFileInfo value) {
        this.convertedFileInfo = value;
    }

    /**
     * Gets the value of the no property.
     * 
     * @return
     *     possible object is
     *     {@link java.math.BigInteger }
     *     
     */
    public int getNumber() {
        return number;
    }

    /**
     * Sets the value of the no property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.math.BigInteger }
     *     
     */
    public void setNumber(int value) {
        this.number = value;
    }

}
