package de.mpg.jena.vo;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;

import thewebsemantic.Id;
import thewebsemantic.Namespace;
import thewebsemantic.RdfProperty;
import thewebsemantic.RdfType;

@Namespace("http://imeji.mpdl.mpg.de/")
@RdfType("mdprofile")
public class MetadataProfile
{
    private Properties properties = new Properties();
    private URI id;
    private String title;
    private String description;
    private Collection<Statement> statements = new LinkedList<Statement>();

    @Id
    public URI getId()
    {
        return id;
    }

    public void setId(URI id)
    {
        this.id = id;
    }

    @RdfProperty("")
    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    @RdfProperty("http://purl.org/dc/elements/1.1/description")
    public String getDescription()
    {
        return description;
    }

    @RdfProperty("http://purl.org/dc/elements/1.1/title")
    public void setDescription(String description)
    {
        this.description = description;
    }

    public Collection<Statement> getStatements()
    {
        return statements;
    }

    public void setStatements(Collection<Statement> statements)
    {
        this.statements = statements;
    }

    public void setProperties(Properties properties)
    {
        this.properties = properties;
    }

    @RdfProperty("http://imeji.mpdl.mpg.de/properties")
    public Properties getProperties()
    {
        return properties;
    }
}
