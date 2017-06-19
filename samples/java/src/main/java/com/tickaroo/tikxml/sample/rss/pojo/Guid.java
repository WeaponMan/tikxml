package com.tickaroo.tikxml.sample.rss.pojo;

import com.tickaroo.tikxml.annotation.Attribute;
import com.tickaroo.tikxml.annotation.TextContent;
import com.tickaroo.tikxml.annotation.Xml;

/**
 * Created by WeaponMan on 6/19/2017.
 */
@Xml(name = "giud")
public class Guid {

    @Attribute(name = "isPermaLink")
    public boolean isPermaLink;

    @TextContent
    public String guid;
}
