package eu.planets_project.ifr.core.services.comparison.comparator.config;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import eu.planets_project.ifr.core.services.comparison.comparator.config.generated.Coco;
import eu.planets_project.ifr.core.services.comparison.comparator.config.generated.CompSet;
import eu.planets_project.ifr.core.services.comparison.comparator.config.generated.Metric;
import eu.planets_project.services.datatypes.Parameter;

/**
 * Access to a complete XCDL comparator config file (Coco), via JAXB-generated classes.
 * @author Fabian Steeg (fabian.steeg@uni-koeln.de)
 */
public final class ComparatorConfigParser {

    private Coco coco;

    /**
     * @param coco The comparator config file
     */
    public ComparatorConfigParser(final File coco) {
        try {
            Unmarshaller unmarshaller = createUnmarshaller();
            java.lang.Object object = unmarshaller.unmarshal(coco);
            System.out.println(object.getClass());
            this.coco = (Coco) object;
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        if (this.coco == null) {
            throw new IllegalArgumentException("Could not load COCO from " + coco);
        }
    }

    /**
     * @param coco The comparator config file
     */
    public ComparatorConfigParser(final InputStream coco) {
        try {
            Unmarshaller unmarshaller = createUnmarshaller();
            java.lang.Object object = unmarshaller.unmarshal(coco);
            System.out.println(object.getClass());
            this.coco = (Coco) object;
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        if (this.coco == null) {
            throw new IllegalArgumentException("Could not load COCO from " + coco);
        }
    }

    private Unmarshaller createUnmarshaller() throws JAXBException {
        JAXBContext jc = JAXBContext
                .newInstance("eu.planets_project.ifr.core.services.comparison.comparator.config.generated");
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        return unmarshaller;
    }

    /**
     * @return The properties of the comparator config file
     */
    public List<Parameter> getProperties() {
        List<Parameter> result = new ArrayList<Parameter>();
        CompSet compSet = coco.getCompSets().get(0);
        // TODO what about multiple files in the Coco?
        List<eu.planets_project.ifr.core.services.comparison.comparator.config.generated.Property> list = compSet
                .getProperties();
        for (eu.planets_project.ifr.core.services.comparison.comparator.config.generated.Property property : list) {
            String name = property.getName();
            StringBuilder metrics = new StringBuilder();
            List<Metric> pcrMetrics = property.getMetrics();
            for (Metric metric : pcrMetrics) {
                int metricId = metric.getId();
                metrics.append("metric").append(" ").append(metric.getName()).append(" ").append(
                        String.valueOf(metricId)).append(",");
            }
            String mString = metrics.toString();
            if (mString.endsWith(",")) {
                mString = mString.substring(0, mString.length() - 1);
            }
            String propId = String.valueOf(property.getId());
            Parameter prop = new Parameter.Builder(name, mString).type(propId).build();
            result.add(prop);
        }
        return result;
    }

    /**
     * @return the full comparator config object tree
     */
    public Coco getCoco() {
        return coco;
    }
}
