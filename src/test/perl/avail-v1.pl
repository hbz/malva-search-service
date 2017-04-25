package Search::PLI;

use strict;
use warnings;
use utf8;

use Sys::Hostname;
use JSON qw(decode_json);
use LWP::UserAgent ();

sub new {
    my ($class) = @_;
    my $self = bless ({}, ref ($class) || $class);
    bless ($self, $class);
    $self->{ua} = LWP::UserAgent->new();
    $self->{url} = "http://" . hostname . ":9500/pli";
    return $self;
}

sub region_avail {
    my ($self, $id, $year) = @_;
    $self->{ua}->default_header('Accept', 'application/json');
    my $url = $self->{url} . '/avail-v1?id=' . $id . '&year=' . $year;
    print STDERR $url, "\n";
    my $content = $self->{ua}->get($url)->decoded_content;
    #print STDERR $content;
    if ($content) {
        my $response = decode_json($content);
        return $response->{meta}->{interlibrarybyregions};
    } else {
        return undef;
    }
}

sub region_select_avail {
    my ($self, $region, $id, $year) = @_;
    $self->{ua}->default_header('Accept', 'application/json');
    my $url = $self->{url} . '/avail-v1?id=' . $id . '&year=' . $year;
    print STDERR $url, "\n";
    my $content = $self->{ua}->get($url)->decoded_content;
    #print STDERR $content;
    if ($content) {
        my $response = decode_json($content);
        my $regions = $response->{meta}->{interlibrarybyregions};
        foreach my $key (@{$regions->{$region})
    } else {
        return undef;
    }
}


sub single_avail {
    my ($self, $region, $isil, $id, $year) = @_;
    $self->{ua}->default_header('Accept', 'application/json');
    my $url = $self->{url} . '/avail-v1/' . $region . '/' . $isil . '?id=' . $id . '&year=' . $year . '&library=' . $isil;
    print STDERR $url, "\n";
    my $content = $self->{ua}->get($url)->decoded_content;
    my $response = decode_json($content);
    my $priority;
    my $info;
    foreach my $key (keys %{$response->{interlibrary}}) {
        my ($library_isil, $library_services) = ($key, $response->{interlibrary}->{$key});
        foreach my $library_service (@$library_services) {
            $priority = $library_service->{priority} unless $priority;
            $info = $library_service unless $info
        }
    }
    return ($priority, $info);
}

package main;

use Data::Dumper;

my $plis = Search::PLI->new();
#print Dumper $plis->single_avail('NRW', 'DE-465', '852471-3', 1991);

print Dumper $plis->all_avail('25098263', 2017);

1;
