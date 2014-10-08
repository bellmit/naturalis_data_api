# Rebuild Specimen and MultiMediaObject document type?
rebuild='false'
if [ "${1}" == 'rebuild' ]
then
  rebuild='true'
fi

# Rename CSV files after they have been processed?
rename='false'
# Number of ES index requests bundled together
bulkRequestSize=1000

libdir='/opt/nda-es-loaders/lib'

# Put load.jar ahead of classpath, so logback settings are
# picked up from there
classpath="${libdir}/nl.naturalis.nda.elasticsearch.load.jar"
for file in `ls ${libdir}`
do
  if [ "${file}" == "nl.naturalis.nda.elasticsearch.load.jar" ]
  then
    continue
  fi
  classpath="${classpath}:${libdir}/${file}"
done
#echo $classpath

JAVA_OPTS="-Xms256m -Xmx1024m"
JAVA_OPTS="$JAVA_OPTS -DdwcaDir=/opt/nda-es-data/col/col-dwca"
JAVA_OPTS="$JAVA_OPTS -Drebuild=${rebuild}"
JAVA_OPTS="$JAVA_OPTS -DbulkRequestSize=${bulkRequestSize}"
JAVA_OPTS="$JAVA_OPTS -Drename=${rename}"

java -cp ${classpath} $JAVA_OPTS nl.naturalis.nda.elasticsearch.load.col.CoLImportAll



