
# perl zoom.pl 'http://host/path?extraRequestData=holdings&recordSchema=mods' 'dc.title = "foobar"'

use strict;
use ZOOM;
my ($spec, $query) = @ARGV;
my $max = 10;
my $options = new ZOOM::Options();
# $options->option( preferredRecordSyntax => 'mods' );
$options->option( sru => 'get' );
$options->option( count => 10);
my $conn = ZOOM::Connection->create($options);
$conn->connect($spec);
my $q = new ZOOM::Query::CQL($query);
my $rs = $conn->search($q);
my $n = $rs->size() > $max ? $max : $rs->size();
print "Records found = ".($rs->size())."\n";
my $rec;
for my $i (1..$n) {
    print "\n\nRecord: $i\n\n";
    $rec = $rs->record_immediate($i-1);
    print $rec->render() if $rec;
}
$rs->destroy();
$conn->destroy();

print "\n\n";

1;