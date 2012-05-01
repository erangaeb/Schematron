package se.pagero.schematron.validation;

import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderFactory;
import se.pagero.schematron.commons.XMLWriter;
import se.pagero.schematron.commons.XsltVersion;
import se.pagero.schematron.filter.ValidationFilter;
import se.pagero.schematron.loader.SchematronLoader;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * SchematronValidator is a processor that checks an XML document against a schematron schema.
 * @author bard.langoy 
 */
public class SchematronValidator extends Validator {

    private ErrorHandler errorHandler;
    private LSResourceResolver resourceResolver;
    private Transformer transformer;

    public SchematronValidator(InputSource inputSource, XsltVersion version, LSResourceResolver resolver) throws TransformerException, IOException, SAXException {
        this.resourceResolver = resolver;

        SchematronLoader loader = new SchematronLoader();
        this.transformer = loader.loadSchema(inputSource, version, resolver);
    }

    @Override
    public void reset() {
        System.out.println("reset method called for SchematronValidator...");
    }

    @Override
    public void validate(Source source, Result result) throws SAXException, IOException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            transformer.setParameter("terminate", "no");
            transformer.transform(source, new StreamResult(baos));

            byte[] reportBytes = baos.toByteArray();
            ValidationFilter filter = createAndInitializeFilter();
            filter.parse(new InputSource(new ByteArrayInputStream(reportBytes)));

            if (!filter.isValid()) {
                throw new SAXException(filter.getErrorReport());
            }
        } catch (TransformerException e) {
            throw new SAXException(e.getMessage(), e);
        }
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    @Override
    public void setResourceResolver(LSResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    @Override
    public LSResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    private ValidationFilter createAndInitializeFilter() throws SAXException {
        ValidationFilter filter = new ValidationFilter();
        filter.setErrorHandler(errorHandler);
        filter.setParent(XMLReaderFactory.createXMLReader());
        filter.setContentHandler(new XMLWriter(new OutputStreamWriter(new ByteArrayOutputStream())));
        filter.setErrorHandler(getErrorHandler());
        return filter;
    }

}
