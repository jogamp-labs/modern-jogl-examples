/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package framework.component;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author gbarbieri
 */
public class Mesh {

    private ArrayList<Attribute> attribs = new ArrayList<>();
    private ArrayList<RenderCmd> renderCmdList = new ArrayList<>();
    private ArrayList<ElementsDrawer> indicesList = new ArrayList<>();
    private ArrayList<ArrayDrawer> arraysList = new ArrayList<>();
    private ArrayList<VAO> namedVaoList = new ArrayList<>();
    private int[] VBO;
    private int[] IBO;
    private int[] vao_;
    private float[] vertexAttributes;
    private int[] vertexIndices;

    public Mesh(String xml, GL3 gl3) {

        readXml(xml);

//        System.out.println("initializeVBO");
        initializeVBO(gl3);
//        System.out.println("initializeVAO");
        initializeVAO(gl3);
    }

    public void render(GL3 gl3) {

//        System.out.println("render()");
        int mode;
        int count;
        int type;
        int indices;
        int first;

        gl3.glBindVertexArray(vao_[0]);
        {
            for (ElementsDrawer elementsDrawer : indicesList) {

                mode = elementsDrawer.getCmd();

                count = elementsDrawer.getIndices().length;

                type = elementsDrawer.getType();

                indices = elementsDrawer.getOffset() * 4;

//                System.out.println("GL3.GL_TRIANGLES: " + GL3.GL_TRIANGLES + " GL3.GL_TRIANGLE_FAN: " + GL3.GL_TRIANGLE_FAN + " GL3.GL_TRIANGLE_STRIP: " + GL3.GL_TRIANGLE_STRIP
//                        + " GL3.GL_UNSIGNED_INT: " + GL3.GL_UNSIGNED_INT);
//                System.out.println("gl3.glDrawElements(" + mode + ", " + count + ", " + type + ", " + indices + ")");
                gl3.glDrawElements(mode, count, type, indices);
            }

            for (ArrayDrawer arrayDrawer : arraysList) {

                mode = arrayDrawer.getCmd();

                first = arrayDrawer.getStart();

                count = arrayDrawer.getCount();

                gl3.glDrawArrays(mode, first, count);
            }
        }
        gl3.glBindVertexArray(0);
    }

    public void render(GL3 gl3, String vaoName) {

        int mode;
        int first;
        int count;
        int type;
        int indices;

        for (int i = 0; i < vao_.length; i++) {

            if (namedVaoList.get(i).getName().equals(vaoName)) {
//                System.out.println("VAO[" + i + "]");
                gl3.glBindVertexArray(vao_[i]);
                {
                    for (ElementsDrawer elementsDrawer : indicesList) {

                        mode = elementsDrawer.getCmd();

                        count = elementsDrawer.getIndices().length;

                        type = elementsDrawer.getType();

                        indices = elementsDrawer.getOffset() * 4;

                        gl3.glDrawElements(mode, count, type, indices);
                    }

                    for (ArrayDrawer arrayDrawer : arraysList) {

                        mode = arrayDrawer.getCmd();

                        first = arrayDrawer.getStart();

                        count = arrayDrawer.getCount();

                        gl3.glDrawArrays(mode, first, count);
                    }
                }
                gl3.glBindVertexArray(0);
            }
        }
    }

    private void readXml(String xml) {

        InputStream inputStream = getClass().getResourceAsStream(xml);
        int offset = 0;

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            Document document = documentBuilder.parse(inputStream);

            Element rootElement = document.getDocumentElement();

            /**
             * Attributes.
             */
            NodeList nodeList = rootElement.getElementsByTagName("attribute");

            if (nodeList != null && nodeList.getLength() > 0) {

                for (int i = 0; i < nodeList.getLength(); i++) {

//                    System.out.println("found attribute " + i);
                    Element element = (Element) nodeList.item(i);

//                    Attribute attribute = new Attribute(element);
                    Attribute attribute = Attribute.create(element);

                    attribute.setOffset(offset);

//                    System.out.println("offset: " + offset);
//                    offset += attribute.getContent().length;
                    offset += attribute.getDataArray_().capacity();

                    attribs.add(attribute);

//                    for (int j = 0; j < attribute.getContent().length / attribute.getSize(); j++) {
//                        System.out.println("attribute.getContent()[" + j + "]:");
//                        for (int k = 0; k < attribute.getSize(); k++) {
//                            System.out.print(" " + attribute.getContent()[j * attribute.getSize() + k]);
//                        }
//                        System.out.println("");
//                    }
                }
            } else {
                throw new Error("`mesh` node must have at least one `attribute` child. File: " + xml);
            }

            /**
             * VAOs.
             */
            nodeList = rootElement.getElementsByTagName("vao");

            if (nodeList != null && nodeList.getLength() > 0) {

                for (int i = 0; i < nodeList.getLength(); i++) {

                    Element element = (Element) nodeList.item(i);

                    VAO vao = VAO.process(element);

                    namedVaoList.add(vao);
                }
            }

            /**
             * Indices.
             */
            offset = 0;

            nodeList = rootElement.getElementsByTagName("indices");

            if (nodeList != null && nodeList.getLength() > 0) {

                for (int i = 0; i < nodeList.getLength(); i++) {

//                    System.out.println("found indicesList " + i);
                    Element element = (Element) nodeList.item(i);

                    ElementsDrawer elementsDrawer = new ElementsDrawer(element);

                    elementsDrawer.setOffset(offset);

//                    System.out.println("offset: " + offset);
                    offset += elementsDrawer.getIndices().length;

                    indicesList.add(elementsDrawer);

//                    for (int j = 0; j < elementsDrawer.getIndices().length / elementsDrawer.getSize(); j++) {
//                        System.out.println("attribute.getContent()[" + j + "]:");
//                        for (int k = 0; k < attribute.getSize(); k++) {
//                            System.out.print(" " + attribute.getContent()[j * 3 + k]);
//                        }
//                        System.out.println("");
//                    }
                }
            }

            /**
             * Arrays.
             */
            nodeList = rootElement.getElementsByTagName("arrays");

            if (nodeList != null && nodeList.getLength() > 0) {

                for (int i = 0; i < nodeList.getLength(); i++) {

                    Element element = (Element) nodeList.item(i);

                    ArrayDrawer arrayDrawer = new ArrayDrawer(element);

                    arraysList.add(arrayDrawer);
                }
            }

        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(Mesh.class.getName()).log(Level.SEVERE, "Could not find the mesh file: " + xml, ex);
        }
    }

    private void initializeVBO(GL3 gl3) {

        VBO = new int[1];

        gl3.glGenBuffers(1, IntBuffer.wrap(VBO));

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, VBO[0]);
        {
            vertexAttributes = putAttributesInFloatArray();

            gl3.glBufferData(GL3.GL_ARRAY_BUFFER, vertexAttributes.length * 4, GLBuffers.newDirectFloatBuffer(vertexAttributes), GL3.GL_STATIC_DRAW);
        }
        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);

        if (!indicesList.isEmpty()) {

            IBO = new int[1];

            gl3.glGenBuffers(1, IntBuffer.wrap(IBO));

            gl3.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, IBO[0]);
            {
                vertexIndices = putIndicesInIntArray();

                gl3.glBufferData(GL3.GL_ELEMENT_ARRAY_BUFFER, vertexIndices.length * 4, GLBuffers.newDirectIntBuffer(vertexIndices), GL3.GL_STATIC_DRAW);
            }

            gl3.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, 0);
        }
    }

    private void initializeVAO(GL3 gl3) {

//        if (VAOList.isEmpty()) {
        ArrayList<Integer> attribList = new ArrayList<>();

        for (Attribute attribute : attribs) {

            attribList.add(attribute.getIndex());
        }

        namedVaoList.add(0, new VAO(attribList));
//        }

        vao_ = new int[namedVaoList.size()];

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, VBO[0]);

        gl3.glGenVertexArrays(vao_.length, IntBuffer.wrap(vao_));

        for (int vaoIndex = 0; vaoIndex < vao_.length; vaoIndex++) {
//            System.out.println("VAO " + vaoIndex);
            VAO vao = namedVaoList.get(vaoIndex);

            gl3.glBindVertexArray(this.vao_[vaoIndex]);
            {
                for (Attribute attribute : attribs) {
//                    System.out.println("attribute: " + attribute.getIndex());
                    for (int index : vao.getAttribList()) {
//                        System.out.println("found: " + index);
                        if (index == attribute.getIndex()) {

//                            int index = attribute.getIndex();
//                            System.out.println("gl3.glEnableVertexAttribArray(" + index + ")");
                            gl3.glEnableVertexAttribArray(index);
//                            System.out.println("gl3.glVertexAttribPointer(" + index + ", " + attribute.getSize() + ", " + GL3.GL_FLOAT + ", false, 0, " + attribute.getOffset() + ")");
                            gl3.glVertexAttribPointer(index, attribute.getSize(), GL3.GL_FLOAT, false, 0, attribute.getOffset());
                        }
                    }
                }
                if (!indicesList.isEmpty()) {
                    gl3.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, IBO[0]);
                }
            }
            gl3.glBindVertexArray(0);
        }
    }

    private int getAttributesTotalSize() {

        int totalSize = 0;

        for (Attribute attribute : attribs) {

            totalSize += attribute.getContent().length;
        }

        return totalSize;
    }

    private float[] putAttributesInFloatArray() {
        float[] fa = new float[getAttributesTotalSize()];

        int indexOffset = 0;

        for (Attribute attribute : attribs) {

            for (int i = 0; i < attribute.getContent().length; i++) {

                fa[indexOffset] = attribute.getContent()[i];
                indexOffset++;
            }
        }

        return fa;
    }

    private int[] putIndicesInIntArray() {
        int[] ia = new int[getIndicesTotalSize()];

        int indexOffset = 0;

        for (ElementsDrawer elementsDrawer : indicesList) {

            for (int i = 0; i < elementsDrawer.getIndices().length; i++) {
                ia[indexOffset] = elementsDrawer.getIndices()[i];
                indexOffset++;
            }
        }

        return ia;
    }

    private int getIndicesTotalSize() {

        int totalSize = 0;

        for (ElementsDrawer elementsDrawer : indicesList) {

            totalSize += elementsDrawer.getIndices().length;
        }

        return totalSize;
    }

    public ArrayList<Attribute> getAttributes() {
        return attribs;
    }

    public ArrayList<ElementsDrawer> getIndicesList() {
        return indicesList;
    }

    public int[] getVBO() {
        return VBO;
    }

    public int[] getIBO() {
        return IBO;
    }

    public int[] getVAO() {
        return vao_;
    }
}
