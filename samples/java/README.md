Java RSS reader/writer sample
===========
Reads rss xml files and provides stdout output for each parsed rss file.
If passed rss is successfully parsed into pojo then program will try to write to disk as temporary file.

__To setup annotation processing in IntelliJ with plugin net.ltgt.apt:__  
Settings... -> Build, Execution, Deployment -> Build tools -> Gradle -> Runner -> check option to delegate build to Gradle

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
wget http://url.to.rss/archive/1 > rss1.xml
wget http://url.to.rss/archive/2 > rss2.xml
java -jar build/libs/rss-reader-1.0-SNAPSHOT-all.jar rss1.xml rss2.xml
```
