//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-558 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.06.30 at 01:03:16 PM GMT+01:00 
//


package eu.planets_project.fedora.planetsdatastream;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for planetsdatastreamType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="planetsdatastreamType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="filedatastream" type="{}filedatastreamType"/>
 *         &lt;element name="title" type="{}titleType"/>
 *         &lt;element name="metadatastream" type="{}metadatastreamType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "planetsdatastreamType", propOrder = {
    "filedatastream",
    "title",
    "metadatastream"
})
public class PlanetsdatastreamType {

    @XmlElement(required = true)
    protected FiledatastreamType filedatastream;
    @XmlElement(required = true)
    protected TitleType title;
    @XmlElement(required = true)
    protected MetadatastreamType metadatastream;

    /**
     * Gets the value of the filedatastream property.
     * 
     * @return
     *     possible object is
     *     {@link FiledatastreamType }
     *     
     */
    public FiledatastreamType getFiledatastream() {
        return filedatastream;
    }

    /**
     * Sets the value of the filedatastream property.
     * 
     * @param value
     *     allowed object is
     *     {@link FiledatastreamType }
     *     
     */
    public void setFiledatastream(FiledatastreamType value) {
        this.filedatastream = value;
    }

    /**
     * Gets the value of the title property.
     * 
     * @return
     *     possible object is
     *     {@link TitleType }
     *     
     */
    public TitleType getTitle() {
        return title;
    }

    /**
     * Sets the value of the title property.
     * 
     * @param value
     *     allowed object is
     *     {@link TitleType }
     *     
     */
    public void setTitle(TitleType value) {
        this.title = value;
    }

    /**
     * Gets the value of the metadatastream property.
     * 
     * @return
     *     possible object is
     *     {@link MetadatastreamType }
     *     
     */
    public MetadatastreamType getMetadatastream() {
        return metadatastream;
    }

    /**
     * Sets the value of the metadatastream property.
     * 
     * @param value
     *     allowed object is
     *     {@link MetadatastreamType }
     *     
     */
    public void setMetadatastream(MetadatastreamType value) {
        this.metadatastream = value;
    }

}