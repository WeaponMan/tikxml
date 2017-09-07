Java RSS reader/writer sample
===========
Reads rss xml files and provides stdout output for each parsed rss file.
If passed rss is successfully parsed into pojo then program will try to write to disk as temporary file.

__How to run on commandline:__

Build with:
```bash
./gradlew shadowJar
```

Run it with:
```bash
java -jar build/libs/rss-reader-1.0-SNAPSHOT-all.jar rss-sample.xml
```
or

```bash
curl http://url.to.rss/archive/1 > rss1.xml
curl http://url.to.rss/archive/2 > rss2.xml
java -jar build/libs/rss-reader-1.0-SNAPSHOT-all.jar rss1.xml rss2.xml
```
