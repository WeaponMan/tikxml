package com.tickaroo.tikxml.sample.rss.pojo;

import com.tickaroo.tikxml.annotation.Attribute;
import com.tickaroo.tikxml.annotation.Xml;

/**
 * Created by WeaponMan on 6/19/2017.
 */
@Xml(name = "media:thumbnail")
public class MediaThumbnail {
    @Attribute(name = "width")
    public int width;

    @Attribute(name = "height")
    public int height;

    @Attribute(name = "url")
    public String url;
}
