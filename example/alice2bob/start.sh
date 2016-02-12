
java -Dconfig.file=settings.conf -jar $1  &

( cd outgoing && inkscape calculation.svg ) &
