package com.tickaroo.tikxml.sample.rss.pojo;

import com.tickaroo.tikxml.annotation.Attribute;
import com.tickaroo.tikxml.annotation.Element;
import com.tickaroo.tikxml.annotation.Xml;

/**
 * Created by WeaponMan on 6/19/2017.
 */

@Xml(name = "rss", writeNamespaces = {
        "media=http://search.yahoo.com/mrss/"
})
public class RSSFeed {

    @Attribute(name = "version")
    public double version;

    @Element(name = "channel")
    public RSSChannel channel;

}
