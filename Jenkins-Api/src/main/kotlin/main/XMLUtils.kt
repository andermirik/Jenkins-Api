package main

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.SAXException
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class XMLUtils {
    companion object {
        @Throws(
            ParserConfigurationException::class,
            SAXException::class,
            IOException::class,
            TransformerException::class
        )
        fun deleteJobParameterInConfigXml(configXml: String, parameterName: String): String {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val input = ByteArrayInputStream(configXml.toByteArray(charset("UTF-8")))
            val doc = builder.parse(input)
            val parameterDefinitions = doc.getElementsByTagName("hudson.model.StringParameterDefinition")
            for (i in 0 until parameterDefinitions.length) {
                val parameter = parameterDefinitions.item(i) as Element
                val paramName = parameter.getElementsByTagName("name").item(0).textContent
                if (paramName == parameterName) {
                    parameter.parentNode.removeChild(parameter)
                    break
                }
            }
            return documentToText(doc);
        }

        @Throws(
            ParserConfigurationException::class,
            SAXException::class,
            IOException::class,
            TransformerException::class
        )
        fun addJobParameterInConfigXml(configXml: String, parameterName: String, defaultValue: String): String {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val input = ByteArrayInputStream(configXml.toByteArray(charset("UTF-8")))
            val doc = builder.parse(input)
            val properties = doc.getElementsByTagName("properties").item(0) as Element
            val parameterDefinition = doc.createElement("hudson.model.StringParameterDefinition")
            val nameElement = doc.createElement("name")
            nameElement.textContent = parameterName
            parameterDefinition.appendChild(nameElement)
            val defaultValueElement = doc.createElement("defaultValue")
            defaultValueElement.textContent = defaultValue
            parameterDefinition.appendChild(defaultValueElement)
            properties.appendChild(parameterDefinition)
            return documentToText(doc);
        }

        @Throws(ParserConfigurationException::class, SAXException::class, IOException::class)
        fun updateJobParameterInConfigXml(
            configXml: String,
            parameterName: String,
            parameterValue: String
        ): String {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val input = ByteArrayInputStream(configXml.toByteArray(charset("UTF-8")))
            val doc = builder.parse(input)
            val parameterDefinitions = doc.getElementsByTagName("hudson.model.StringParameterDefinition")
            for (i in 0 until parameterDefinitions.length) {
                val parameter = parameterDefinitions.item(i) as Element
                val paramName = parameter.getElementsByTagName("name").item(0).textContent
                if (paramName == parameterName) {
                    parameter.getElementsByTagName("defaultValue").item(0).textContent = parameterValue
                    break
                }
            }
            return documentToText(doc);
        }

        private fun documentToText(document: Document): String {
            val transformerFactory = TransformerFactory.newInstance()
            val transformer: Transformer = transformerFactory.newTransformer()
            val source = DOMSource(document)
            val stringWriter = StringWriter()
            val result = StreamResult(stringWriter)
            transformer.transform(source, result)
            return stringWriter.buffer.toString()
        }

        fun generateViewConfigXml(viewName: String, viewType: String, jobNames: List<String>): String {
            val jobNamesXml = jobNames.joinToString(separator = "") { "<string>$it</string>" }
            return when (viewType.toLowerCase()) {
                "listview" -> {
                    val propertyList = "$" + "PropertyList"
                    """<hudson.model.ListView>
                    <name>$viewName</name>
                    <filterExecutors>false</filterExecutors>
                    <filterQueue>false</filterQueue>
                    <properties class="hudson.model.View$propertyList"/>
                    <jobNames>
                        <comparator class="hudson.util.CaseInsensitiveComparator"/>
                        $jobNamesXml
                    </jobNames>
                    <jobFilters/>
                    <columns>
                        <hudson.views.StatusColumn/>
                        <hudson.views.WeatherColumn/>
                        <hudson.views.JobColumn/>
                        <hudson.views.LastSuccessColumn/>
                        <hudson.views.LastFailureColumn/>
                        <hudson.views.LastDurationColumn/>
                        <hudson.views.BuildButtonColumn/>
                    </columns>
                    <recurse>false</recurse>
                </hudson.model.ListView>"""
                }
                // Add more view types here if needed
                else -> throw IllegalArgumentException("Unknown view type: $viewType")
            }
        }

        @Throws(ParserConfigurationException::class, SAXException::class, IOException::class)
        fun parseJobParametersFromConfigXml(configXml: String): Map<String, String> {
            val parameters: MutableMap<String, String> = HashMap()
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val input = ByteArrayInputStream(configXml.toByteArray(charset("UTF-8")))
            val doc = builder.parse(input)
            val parameterDefinitions = doc.getElementsByTagName("hudson.model.StringParameterDefinition")
            for (i in 0 until parameterDefinitions.length) {
                val parameter = parameterDefinitions.item(i) as Element
                val paramName = parameter.getElementsByTagName("name").item(0).textContent
                val paramValue = parameter.getElementsByTagName("defaultValue").item(0).textContent
                parameters[paramName] = paramValue
            }
            return parameters
        }
    }
}