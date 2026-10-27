package org.xmlpull.v1;
public interface XmlSerializer {
    void setOutput(java.io.OutputStream os, String encoding) throws java.io.IOException;
    void setOutput(java.io.Writer writer) throws java.io.IOException;
    void startDocument(String encoding, Boolean standalone) throws java.io.IOException;
    void endDocument() throws java.io.IOException;
    XmlSerializer startTag(String namespace, String name) throws java.io.IOException;
    XmlSerializer attribute(String namespace, String name, String value) throws java.io.IOException;
    XmlSerializer endTag(String namespace, String name) throws java.io.IOException;
    XmlSerializer text(String text) throws java.io.IOException;
    void flush() throws java.io.IOException;
    void setFeature(String name, boolean state); void setPrefix(String prefix, String namespace) throws java.io.IOException;
}
