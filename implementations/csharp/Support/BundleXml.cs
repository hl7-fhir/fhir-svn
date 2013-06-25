﻿/*
  Copyright (c) 2011-2012, HL7, Inc.
  All rights reserved.
  
  Redistribution and use in source and binary forms, with or without modification, 
  are permitted provided that the following conditions are met:
  
   * Redistributions of source code must retain the above copyright notice, this 
     list of conditions and the following disclaimer.
   * Redistributions in binary form must reproduce the above copyright notice, 
     this list of conditions and the following disclaimer in the documentation 
     and/or other materials provided with the distribution.
   * Neither the name of HL7 nor the names of its contributors may be used to 
     endorse or promote products derived from this software without specific 
     prior written permission.
  
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
  INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
  POSSIBILITY OF SUCH DAMAGE.
  

*/

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Xml;
using Hl7.Fhir.Model;
using System.Xml.Linq;
using Hl7.Fhir.Parsers;
using System.IO;
using Hl7.Fhir.Serializers;

namespace Hl7.Fhir.Support
{
    internal static class BundleXml
    {
        public const string XATOM_FEED = "feed";
        public const string XATOM_DELETED_ENTRY = "deleted-entry";
        public const string XATOM_DELETED_WHEN = "when";
        public const string XATOM_DELETED_REF = "ref";
        public const string XATOM_LINK = "link";
        public const string XATOM_LINK_REL = "rel";
        public const string XATOM_LINK_HREF = "href";
        public const string XATOM_CONTENT_BINARY = "Binary";
        public const string XATOM_CONTENT_TYPE = "type";
        public const string XATOM_CONTENT_BINARY_TYPE = "contentType";
        public const string XATOM_TITLE = "title";
        public const string XATOM_UPDATED = "updated";
        public const string XATOM_ID = "id";
        public const string XATOM_ENTRY = "entry";
        public const string XATOM_PUBLISHED = "published";
        public const string XATOM_AUTHOR = "author";
        public const string XATOM_AUTH_NAME = "name";
        public const string XATOM_AUTH_URI = "uri";
        public const string XATOM_CATEGORY = "category";
        public const string XATOM_CAT_TERM = "term";
        public const string XATOM_CAT_SCHEME = "scheme";
        public const string XATOM_CAT_LABEL = "label";
        public const string XATOM_CONTENT = "content";
        public const string XATOM_SUMMARY = "summary";
        public const string XATOM_TOTALRESULTS = "totalResults";

        public static string ATOM_CATEGORY_RESOURCETYPE_NS = "http://hl7.org/fhir/resource-types";
        public static string ATOMPUB_TOMBSTONES_NS = "http://purl.org/atompub/tombstones/1.0";
        public static string ATOMPUB_NS = "http://www.w3.org/2005/Atom";
        public static string OPENSEARCH_NS = "http://a9.com/-/spec/opensearch/1.1";

        private static readonly XNamespace XATOMNS = ATOMPUB_NS;
        private static readonly XNamespace XTOMBSTONE = ATOMPUB_TOMBSTONES_NS;
        private static readonly XNamespace XFHIRNS = Util.FHIRNS;
        private static readonly XNamespace XOPENSEARCHNS = OPENSEARCH_NS;

        private static string xValue(XObject elem)
        {
            if (elem == null) return null;

            if (elem is XElement)
                return (elem as XElement).Value;
            if (elem is XAttribute)
                return (elem as XAttribute).Value;

            return null;
        }

        private static string stringValueOrNull(XObject elem)
        {
            string value = xValue(elem);

            return String.IsNullOrEmpty(value) ? null : value;
        }

        private static int? intValueOrNull(XObject elem)
        {
            string value = xValue(elem);

            return String.IsNullOrEmpty(value) ? (int?)null : Int32.Parse(value);
        }

        private static Uri uriValueOrNull(XObject elem)
        {
            string value = stringValueOrNull(elem);

            return String.IsNullOrEmpty(value) ? null : new Uri(value, UriKind.RelativeOrAbsolute);
        }

        private static DateTimeOffset? instantOrNull(XObject elem)
        {
            string value = stringValueOrNull(elem);

            return String.IsNullOrEmpty(value) ? (DateTimeOffset?)null : Util.ParseIsoDateTime(value);
        }

        internal static Bundle Load(XmlReader reader, ErrorList errors)
        {
            XElement feed;

            try
            {
                feed = XDocument.Load(reader).Root;
            }
            catch (Exception exc)
            {
                errors.Add("Exception while loading feed: " + exc.Message);
                return null;
            }

            Bundle result;

            try
            {
                result = new Bundle()
                {
                    Title = stringValueOrNull(feed.Element(XATOMNS + XATOM_TITLE)),
                    LastUpdated = instantOrNull(feed.Element(XATOMNS + XATOM_UPDATED)),
                    Id = uriValueOrNull(feed.Element(XATOMNS + XATOM_ID)),
                    Links = getLinks(feed.Elements(XATOMNS + XATOM_LINK)),
                    AuthorName = feed.Elements(XATOMNS + XATOM_AUTHOR).Count() == 0 ? null :
                            stringValueOrNull(feed.Element(XATOMNS + XATOM_AUTHOR)
                                .Element(XATOMNS + XATOM_AUTH_NAME)),
                    AuthorUri = feed.Elements(XATOMNS + XATOM_AUTHOR).Count() == 0 ? null :
                            stringValueOrNull(feed.Element(XATOMNS + XATOM_AUTHOR)
                                .Element(XATOMNS + XATOM_AUTH_URI)),
                    TotalResults = intValueOrNull(feed.Element(XOPENSEARCHNS + XATOM_TOTALRESULTS))
                };
            }
            catch (Exception exc)
            {
                errors.Add("Exception while parsing xml feed attributes: " + exc.Message,
                    String.Format("Feed '{0}'", feed.Element(XATOMNS + XATOM_ID).Value));
                return null;
            }

            result.Entries = loadEntries(feed.Elements().Where(elem =>
                        (elem.Name == XATOMNS + XATOM_ENTRY ||
                         elem.Name == XTOMBSTONE + XATOM_DELETED_ENTRY)), result, errors);

            errors.AddRange(result.Validate());

            return result;
        }

        internal static Bundle Load(string xml, ErrorList errors)
        {
            return Load(Util.XmlReaderFromString(xml), errors);
        }

        private static ManagedEntryList loadEntries(IEnumerable<XElement> entries, Bundle parent, ErrorList errors)
        {
            var result = new ManagedEntryList(parent);

            foreach (var entry in entries)
            {
                result.Add(loadEntry(entry, errors));
            }

            return result;
        }

        internal static BundleEntry LoadEntry(string xml, ErrorList errors)
        {
            return LoadEntry(Util.XmlReaderFromString(xml), errors);
        }

        internal static BundleEntry LoadEntry(XmlReader reader, ErrorList errors)
        {
            XElement entry;

            try
            {
                entry = XDocument.Load(reader).Root;
            }
            catch (Exception exc)
            {
                errors.Add("Exception while loading entry: " + exc.Message);
                return null;
            }

            return loadEntry(entry, errors);
        }

        private static BundleEntry loadEntry(XElement entry, ErrorList errors)
        {
            BundleEntry result;

            errors.DefaultContext = "An atom entry";

            try
            {
                if (entry.Name == XTOMBSTONE + XATOM_DELETED_ENTRY)
                    result = new DeletedEntry();
                else
                    result = new ResourceEntry();

                result.Id = uriValueOrNull(entry.Attribute(XATOM_DELETED_REF));
                if (result.Id != null) errors.DefaultContext = String.Format("Entry '{0}'", result.Id.ToString());

                result.Links = getLinks(entry.Elements(XATOMNS + XATOM_LINK));
                result.Tags = getTags(entry.Elements(XATOMNS + XATOM_CATEGORY));

                if (result is DeletedEntry)
                {
                    ((DeletedEntry)result).When = instantOrNull(entry.Attribute(XATOM_DELETED_WHEN));
                }
                else
                {
                    ResourceEntry re = (ResourceEntry)result;
                    re.Title = stringValueOrNull(entry.Element(XATOMNS + XATOM_TITLE));
                    re.LastUpdated = instantOrNull(entry.Element(XATOMNS + XATOM_UPDATED));
                    re.Published = instantOrNull(entry.Element(XATOMNS + XATOM_PUBLISHED));
                    re.EntryAuthorName = entry.Elements(XATOMNS + XATOM_AUTHOR).Count() == 0 ? null :
                                stringValueOrNull(entry.Element(XATOMNS + XATOM_AUTHOR)
                                    .Element(XATOMNS + XATOM_AUTH_NAME));
                    re.EntryAuthorUri = entry.Elements(XATOMNS + XATOM_AUTHOR).Count() == 0 ? null :
                                stringValueOrNull(entry.Element(XATOMNS + XATOM_AUTHOR)
                                    .Element(XATOMNS + XATOM_AUTH_URI));

                    XElement content = entry.Element(XATOMNS + XATOM_CONTENT);
                    if (content != null) re.Content = getContents(content, errors);
                }
            }
            catch (Exception exc)
            {
                errors.Add("Exception while reading entry: " + exc.Message);
                return null;
            }
            finally
            {
                errors.DefaultContext = null;
            }

            return result;
        }


        private static TagList getTags(IEnumerable<XElement> tags)
        {
            var result = new TagList();

            if (tags != null)
            {
                foreach (var tag in tags)
                {
                    var scheme = stringValueOrNull(tag.Attribute(BundleXml.XATOM_CAT_SCHEME));

                    if (scheme == Tag.TAG_SCHEME)
                    {
                        result.Add(new Tag
                        {
                            Uri = uriValueOrNull(tag.Attribute(BundleXml.XATOM_CAT_TERM)),
                            Label = stringValueOrNull(tag.Attribute(BundleXml.XATOM_CAT_LABEL))
                        });
                    }
                }
            }

            return result;
        }

        private static UriLinkList getLinks(IEnumerable<XElement> links)
        {
            return new UriLinkList(
                links.Select(el => new UriLinkEntry
                {
                    Rel = stringValueOrNull(el.Attribute(XATOM_LINK_REL)),
                    Uri = uriValueOrNull(el.Attribute(XATOM_LINK_HREF))
                }));
        }


        private static Resource getContents(XElement content, ErrorList errors)
        {
            string contentType = stringValueOrNull(content.Attribute(XATOM_CONTENT_TYPE));

            if (contentType != "text/xml")
            {
                errors.Add("Entry should have contents of type 'text/xml'");
                return null;
            }

            //TODO: This is quite inefficient. The Xml parser has just parsed this
            //entry's Resource from text, now we are going to serialize it to as string
            //just to read from it again using a Xml parser. But that is what my
            //parser takes as input, so no choice for now...
            return FhirParser.ParseResourceFromXml(content.FirstNode.ToString(), errors);
        }


        public static void WriteTo(Bundle bundle, XmlWriter writer)
        {
            if (bundle == null) throw new ArgumentException("Bundle cannot be null");

            var root = new XElement(XATOMNS + XATOM_FEED);

            if (!String.IsNullOrWhiteSpace(bundle.Title)) root.Add(xmlCreateTitle(bundle.Title));
            if (Util.UriHasValue(bundle.Id)) root.Add(xmlCreateId(bundle.Id));
            if (bundle.LastUpdated != null) root.Add(new XElement(XATOMNS + XATOM_UPDATED, bundle.LastUpdated));

            if (!String.IsNullOrWhiteSpace(bundle.AuthorName))
                root.Add(xmlCreateAuthor(bundle.AuthorName, bundle.AuthorUri));
            if (bundle.TotalResults != null) root.Add(new XElement(XOPENSEARCHNS + XATOM_TOTALRESULTS, bundle.TotalResults));
            foreach (var l in bundle.Links)
                root.Add(xmlCreateLink(l.Rel, l.Uri));

            foreach (var entry in bundle.Entries)
                root.Add(createEntry(entry));

            var result = new XDocument(root);
            result.WriteTo(writer);
        }

     
        public static void WriteTo(BundleEntry entry, XmlWriter writer)
        {
            if (entry == null) throw new ArgumentException("Entry cannot be null");

            var result = createEntry(entry);

            var doc = new XDocument(result);
            doc.WriteTo(writer);
        }

      

        private static XElement createEntry(BundleEntry entry)
        {
            if (entry is ResourceEntry)
                return createResourceEntry((ResourceEntry)entry);
            else if (entry is DeletedEntry)
                return createDeletedEntry((DeletedEntry)entry);
            else
                throw new ArgumentException("Don't know how to serialize an entry of type " + entry.GetType().ToString());
        }

        private static XElement createDeletedEntry(DeletedEntry de)
        {
            XElement result = new XElement(XTOMBSTONE + XATOM_DELETED_ENTRY);

            if(Util.UriHasValue(de.Id))
               result.Add(new XAttribute(XATOM_DELETED_REF, de.Id.ToString()));

            if(de.When != null)
               result.Add(new XAttribute(XATOM_DELETED_WHEN, de.When));

            if (Util.UriHasValue(de.Links.SelfLink))
                result.Add(new XElement(XATOMNS + XATOM_LINK,
                        new XAttribute(XATOM_LINK_REL, Util.ATOM_LINKREL_SELF),
                        new XAttribute(XATOM_LINK_HREF, de.Links.SelfLink.ToString())));

            return result;
        }


        private static XElement createResourceEntry(ResourceEntry entry)
        {
            //Note: this handles both BinaryEntry and ResourceEntry

            XElement result = new XElement(XATOMNS + XATOM_ENTRY);

            if (!String.IsNullOrEmpty(entry.Title)) result.Add(xmlCreateTitle(entry.Title));
            if (Util.UriHasValue(entry.Id)) result.Add(xmlCreateId(entry.Id));

            if (entry.LastUpdated != null) result.Add(new XElement(XATOMNS + XATOM_UPDATED, entry.LastUpdated.Value));
            if (entry.Published != null) result.Add(new XElement(XATOMNS + XATOM_PUBLISHED, entry.Published.Value));

            if (!String.IsNullOrWhiteSpace(entry.EntryAuthorName))
                result.Add(xmlCreateAuthor(entry.EntryAuthorName, entry.EntryAuthorUri));

            foreach (var l in entry.Links)
                if(l.Uri != null)
                    result.Add(xmlCreateLink(l.Rel, l.Uri));

            if (entry.Content != null)
                result.Add(new XElement(XATOMNS + XATOM_CONTENT,
                    new XAttribute(XATOM_CONTENT_TYPE, "text/xml"),
                    FhirSerializer.SerializeResourceAsXElement(entry.Content)));

            // Note: this is a read-only property, so it is serialized but never parsed
            if (entry.Summary != null)
                result.Add(new XElement(XATOMNS + XATOM_SUMMARY,
                        new XAttribute(XATOM_CONTENT_TYPE, "xhtml"), XElement.Parse(entry.Summary)));

            return result;
        }

        private static XElement xmlCreateId(Uri id)
        {
            return new XElement(XATOMNS + XATOM_ID, id.ToString());
        }

        private static XElement xmlCreateLink(string rel, Uri uri)
        {
            return new XElement(XATOMNS + XATOM_LINK,
                        new XAttribute(XATOM_LINK_REL, rel), new XAttribute(XATOM_LINK_HREF, uri.ToString()));
        }

        private static XElement xmlCreateTitle(string title)
        {
            return new XElement(XATOMNS + XATOM_TITLE, new XAttribute(XATOM_CONTENT_TYPE, "text"), title);
        }


        private static XElement xmlCreateCategory(string name, string scheme)
        {
            var result = new XElement(XATOMNS + XATOM_CATEGORY);

            if (!String.IsNullOrEmpty(name))
                result.Add(new XAttribute(XATOM_CAT_TERM, name));

            if (!String.IsNullOrEmpty(scheme))
                result.Add(new XAttribute(XATOM_CAT_SCHEME, scheme));

            return result;
        }

        private static XElement xmlCreateAuthor(string name, string uri)
        {
            var result = new XElement(XATOMNS + XATOM_AUTHOR);

            if (!String.IsNullOrEmpty(name))
                result.Add(new XElement(XATOMNS + XATOM_AUTH_NAME, name));

            if (!String.IsNullOrEmpty(uri))
                result.Add(new XElement(XATOMNS + XATOM_AUTH_URI, uri));

            return result;
        }
    }
}
