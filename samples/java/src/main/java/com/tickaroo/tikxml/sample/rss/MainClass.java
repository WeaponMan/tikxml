package com.tickaroo.tikxml.sample.rss;

import com.tickaroo.tikxml.TikXml;
import com.tickaroo.tikxml.sample.rss.converter.BooleanConverter;
import com.tickaroo.tikxml.sample.rss.pojo.RSSChannel;
import com.tickaroo.tikxml.sample.rss.pojo.RSSFeed;
import com.tickaroo.tikxml.sample.rss.pojo.RSSImage;
import com.tickaroo.tikxml.sample.rss.pojo.RSSItem;
import okio.BufferedSink;
import okio.Okio;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Created by WeaponMan on 6/19/2017.
 */
public class MainClass {
    public static void main(String[] args) {
        TikXml tikXml = new TikXml.Builder()
                .addTypeConverter(boolean.class, new BooleanConverter())
                .addTypeConverter(Boolean.class, new BooleanConverter())
                .exceptionOnUnreadXml(false)
                .build();

        List<File> files = new ArrayList<>();

        if (args.length > 0) {
            files = Stream.of(args)
                    .map(File::new)
                    .filter(File::exists)
                    .collect(toList());
        }

        if (files.isEmpty()) {
            files.add(new File("rss-sample.xml"));
        }

        List<RSSFeed> feedStream = files.stream()
                .map(file -> parseRSSFeed(tikXml, file))
                .filter(Objects::nonNull)
                .collect(toList());

        feedStream.forEach(MainClass::printRSSFeed);
        System.out.println("Writing xml to STDOUT");
        feedStream.forEach(rssFeed -> writeRSSFeed(tikXml, rssFeed));
    }

    private static void writeRSSFeed(TikXml tikXml, RSSFeed rssFeed) {
        try {
            File temp = File.createTempFile("rss-feed", ".xml");
            System.out.println("Writing to file -> " + temp.getAbsolutePath());
            try (OutputStream stream = new FileOutputStream(temp)) {
                BufferedSink sink = Okio.buffer(Okio.sink(stream));
                tikXml.write(sink, rssFeed);
                sink.flush();
            } catch (IOException e) {
                System.out.println("Unable to write");
            }
        } catch (IOException e) {
            System.out.println("Unable to create temp file");
        }
    }

    private static RSSFeed parseRSSFeed(TikXml tikXml, File file) {
        try {
            return tikXml.read(Okio.buffer(Okio.source(file)), RSSFeed.class);
        } catch (IOException e) {
            System.out.println("Unable to read XML into Pojo!");
            e.printStackTrace();
        }
        return null;
    }

    private static void printItemInfo(RSSItem rssItem) {
        System.out.println("--> Item:");
        if (rssItem.guid != null) {
            System.out.println("---> guid        = " + rssItem.guid.guid + "(isPermaLink " + rssItem.guid.isPermaLink + ")");
        }
        System.out.println("---> link        = " + rssItem.link);
        System.out.println("---> title       = " + rssItem.title);
        System.out.println("---> author      = " + rssItem.author);
        System.out.println("---> published   = " + rssItem.pubDate);
        System.out.println("---> description = " + rssItem.description);
    }

    private static void printImageInfo(RSSImage rssImage) {
        System.out.println("--> Image:");
        System.out.println("---> link   = " + rssImage.link);
        System.out.println("---> title  = " + rssImage.title);
        System.out.println("---> url    = " + rssImage.url);
        System.out.println("---> width  = " + rssImage.width);
        System.out.println("---> height = " + rssImage.height);
    }

    private static void printChannelInfo(RSSChannel rssChannel) {
        System.out.println("-> Channel: ");
        System.out.println("--> title       = " + rssChannel.title);
        System.out.println("--> link        = " + rssChannel.link);
        System.out.println("--> description = " + rssChannel.description);
        System.out.println("--> language    = " + rssChannel.language);
        System.out.println("--> published   = " + rssChannel.pubDate);
        if (rssChannel.image != null) {
            printImageInfo(rssChannel.image);
        }
        if (rssChannel.items != null) {
            rssChannel.items.forEach(MainClass::printItemInfo);
        }
    }

    private static void printRSSFeed(RSSFeed rssFeed) {
        System.out.println("--- RSS START ---");
        System.out.println("-> version = " + rssFeed.version);
        if (rssFeed.channel != null) {
            printChannelInfo(rssFeed.channel);
        }
        System.out.println("--- RSS END ---");
    }
}
