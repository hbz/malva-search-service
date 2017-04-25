package Search::PLI;

use strict;
use warnings;
use utf8;

use JSON qw(decode_json);
use LWP::UserAgent ();
use Data::Dumper;

sub new {
    my ($class) = @_;
    my $self = bless ({}, ref ($class) || $class);
    bless ($self, $class);
    $self->{ua} = LWP::UserAgent->new();
    $self->{url} = "http://10.3.2.31:9500/pli";
    return $self;
}

sub region_avail {
    my ($self, $id, $year) = @_;
    $self->{ua}->default_header('Accept', 'application/json');
    my $url = $self->{url} . '/avail-v1?id=' . $id . '&year=' . $year;
    my $content = $self->{ua}->get($url)->decoded_content;
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
    my $content = $self->{ua}->get($url)->decoded_content;
    if ($content) {
        my $response = decode_json($content);
        my $regions = $response->{meta}->{interlibrarybyregions};
        foreach my $key (@{$regions->{$region}}) {

        }
    } else {
        return undef;
    }
}


sub single_avail {
    my ($self, $region, $isil, $id, $year) = @_;
    $self->{ua}->default_header('Accept', 'application/json');
    my $url = $self->{url} . '/avail-v1/' . $region . '/' . $isil . '?id=' . $id . '&year=' . $year . '&library=' . $isil;
    my $content = $self->{ua}->get($url)->decoded_content;
    my $response = decode_json($content);
    my $priority;
    my $info = '';
    foreach my $key (keys %{$response->{interlibrary}}) {
        my ($library_isil, $library_services) = ($key, $response->{interlibrary}->{$key});
        foreach my $library_service (@$library_services) {
            $priority = $library_service->{priority} unless $priority;
            $info = $self->append_info($info, $library_service);
        }
    }
    return ($priority, $info);
}

sub append_info {
    my ($self, $response, $service) = @_;
    $response .= 'LP' . $service->{priority} . ": ";
    if ($service->{type} eq 'none') {
        $response .= ' keine Fernleihe erlaubt ';
    } else {
        $response .=  $self->carriertype->{$service->{carriertype}};
        my $modes = ref $service->{mode} eq 'ARRAY' ? $service->{mode} : [ $service->{mode} ] ;
        foreach my $mode (@$modes) {
            if ($mode ne 'copy') {
                $response .= ' - ' . $self->servicemode->{$mode};
            }
        }
        if ($service->{priority} < 3) {
            $response .= ' - beschleunigte Übertragung';
        }
        if ($service->{distribution}) {
            my $distributions = ref $service->{distribution} eq 'ARRAY' ? $service->{distribution} : [ $service->{distribution} ] ;
            foreach my $distribution (@$distributions) {
                $response .= ' - ' . $self->servicedistribution->{$distribution};
            }
        }
        if ($service->{comment}) {
            $response .= ' (' . $service->{comment} . ')';
        }
    }
    $response .= "\n";
    return $response;
}

sub carriertype {
    return {
        'online resource' => 'Online-Ressource',
        'volume' => 'gedruckte Ressource',
        'computer disc' => 'CD/DVD',
        'computer tape cassette' => 'Computer-Kassette',
        'computer chip cartridge' => 'Computer-Steckmodul',
        'microform' => 'Mikroform',
        'other' => 'sonstiges Medium'
    }
}

sub servicetype {
    return {
        'interlibrary' => 'Leihverkehr',
        'none' => 'Kein Leihverkehr'
    }
}

sub servicemode {
    return {
        'copy' => 'Kopie',
        'loan' => 'Leihe',
        'none' => 'Keine Kopie/Leihe',
        'electronic' => 'Kopie mit elektronischem Versand'
    }
}

sub servicedistribution {
    return {
        'postal' => 'Papierkopie an Bestellbibliothek',
        'domestic' => 'nur Inland',
        'electronic' => 'auch elektronisch',
        'unrestricted' => '',
        'none' => ''
    }
}

package main;

use Data::Dumper;

my $pli = Search::PLI->new();
print Dumper $pli->single_avail('NRW', 'DE-465', '852471-3', 1991);

#print Dumper $pli->region_avail('25098263', 2017);

1;
