  # Generating Music dataset

### Download Music Brainz Database Dump

MapR-Music application uses music dataset, which is obtained by denormalization of 
[Music Brainz](https://musicbrainz.org/) Database dump. So, the first thing you have to do is to download Database dump:
* Follow [Music Brainz Download page link](https://musicbrainz.org/doc/MusicBrainz_Database/Download)
* The data dumps are available for download via http, ftp or rsync at different mirrors. Choose the most appropriate way
and download the dump. For example to get latest version of dump via http or ftp protocols:

```
$ export MB_MIRROR='https://mirrors.dotsrc.org/MusicBrainz/data/fullexport/'
$ export MB_LATEST=`curl -X GET $MB_MIRROR/LATEST`
$ wget $MB_MIRROR/$MB_LATEST/mbdump.tar.bz2
$ wget $MB_MIRROR/$MB_LATEST/mbdump-derived.tar.bz2
```

* Extract archive after download completes:

```
$ tar xvjf mbdump.tar.bz2
```

* Extract `mbdump-derived.tar.bz2` archive, which contains additional meta information:
```
$ tar xvjf mbdump-derived.tar.bz2
```

### Use dump conversion util

At this point you can use dump conversion util to convert Music Brainz Database dump into set of JSON files, that are 
ready to import into MapR-DB.

1. Go to conversion util directory: `cd mapr-music/data`.
2. Build and package conversion util: `mvn clean package`
3. Run the util. Note, that it takes three arguments:

* -s | --src - Required argument which is used to point to Brainz Database dump directory, which contains table data.
* -d | --dst - Required argument which is used to point to dataset output directory. Dump conversion util will 
create two directories at this specified output directory for storing albums/artists JSON documents .
* -n | --num - Optional argument which specifies number of artists documents that will be created. Default value is 
10_000. Note, that number of albums documents may differ from number of artists documents, since one artist can have 
none or several albums.
* -c | --chosen - Optional argument. When specified only data with images will be converted. Note that execution time 
will be raised.
* -h | --help - Prints usage information.

```
$ cd target
$ java -jar mb-dump-converter-1.0-SNAPSHOT.jar --src /path/to/dump/mbdump --dst /path/to/output/ --num 1000 --chosen
```

4. Check output directory for generated data:

```
$ cd /path/to/output/
$ ls
```

You will be able to see `artists` and `albums` directories that contain JSON documents with artists and albums data 
respectively.

### Import data in MapR-DB

* Copy the newly generated dataset to one of the nodes of your cluster:

```
$ scp -r /path/to/output/artists /path/to/output/albums /path/to/output/languages youruser@nodehostname:/dataset/path/at/node
```

* Load data into MapR-FS:
```
$ hadoop fs -copyFromLocal /path/to/output/albums /tmp/albums
$ hadoop fs -copyFromLocal /path/to/output/artists /tmp/artists
$ hadoop fs -copyFromLocal /path/to/output/languages /tmp/languages
$ hadoop fs -copyFromLocal /path/to/output/users /tmp/users
$ hadoop fs -copyFromLocal /path/to/output/ratings-albums /tmp/albums_ratings
$ hadoop fs -copyFromLocal /path/to/output/ratings-artists /tmp/artists_ratings
```

* Create tables:

```
$ maprcli table create -path /apps/albums -tabletype json
$ maprcli table create -path /apps/artists -tabletype json
$ maprcli table create -path /apps/languages -tabletype json
$ maprcli table create -path /apps/albums_ratings -tabletype json
$ maprcli table create -path /apps/artists_ratings -tabletype json
$ maprcli table create -path /apps/users -tabletype json
$ maprcli table create -path /apps/statistics -tabletype json
$ maprcli table create -path /apps/recommendations -tabletype json

```

* Import data into MapR-DB using `importJSON` tool:
```
$ mapr importJSON -idField _id -src /tmp/albums/* -dst /apps/albums -mapreduce false
$ mapr importJSON -idField _id -src /tmp/artists/* -dst /apps/artists -mapreduce false
$ mapr importJSON -idField _id -src /tmp/languages/* -dst /apps/languages -mapreduce false
$ mapr importJSON -idField _id -src /tmp/users/* -dst /apps/users -mapreduce false
$ mapr importJSON -idField _id -src /tmp/ratings-albums/* -dst /apps/albums_ratings -mapreduce false
$ mapr importJSON -idField _id -src /tmp/ratings-artists/* -dst /apps/artists_ratings -mapreduce false
```

Note: in case of lack of memory while importing ratings documents try to split ratings into multiple sets and import 
them one by one.

* Change table permissions to allow access for the MapR-Music application:
```
$ maprcli table cf edit -path /apps/albums -cfname default -readperm p -writeperm p -traverseperm  p
$ maprcli table cf edit -path /apps/artists -cfname default -readperm p -writeperm p -traverseperm  p
$ maprcli table cf edit -path /apps/languages -cfname default -readperm p -writeperm p -traverseperm  p
$ maprcli table cf edit -path /apps/albums_ratings -cfname default -readperm p -writeperm p -traverseperm  p
$ maprcli table cf edit -path /apps/artists_ratings -cfname default -readperm p -writeperm p -traverseperm  p
$ maprcli table cf edit -path /apps/users -cfname default -readperm p -writeperm p -traverseperm  p
$ maprcli table cf edit -path /apps/statistics -cfname default -readperm p -writeperm p -traverseperm  p
$ maprcli table cf edit -path /apps/recommendations -cfname default -readperm p -writeperm p -traverseperm  p
```

After that dataset is ready to be used by MapR-Music application.
