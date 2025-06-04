#!/usr/bin/perl -w
use strict;

die "Usage: $0 <core location> <pc field>"
   unless $#ARGV == 1;

my $SYMFILE = $ARGV[0];
my $PCPOS = $ARGV[1];

while (<STDIN>) {
    s/^\s+//;
    s/\s+$//;
    print "$_\n";
    my @l = split(/\s+/, $_);
    print `atos -o $SYMFILE $l[$PCPOS]`;
    print "\n";
}
