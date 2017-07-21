#!/usr/bin/env bash

usage() {
    echo usage: $0 path/to/sonar-scanner
    exit 1
}

test -f "$1" && test -x "$1" || usage

scanner=$1

if type mktemp &>/dev/null; then
    tempdir=$(mktemp -d)
    tempdir=$(cd "$tempdir"; pwd -P)
else
    tempdir=/tmp/"$(basename "$0")-$$"
    mkdir -p "$tempdir"
fi

cleanup() {
    rm -fr "$tempdir"
}

trap 'cleanup; exit 1' 1 2 3 15
trap 'cleanup; exit 0' 0

abspath() {
    (cd "$(dirname "$1")"; echo $PWD/"$(basename "$1")")
}

verify() {
    printf '%s -> ' "$1"
    shift
    "$@" &>/dev/null && echo ok || echo failed
}

relpath_to_root() {
    (
    cd "$1"
    relpath=.
    while test "$PWD" != /; do
        cd ..
        relpath=$relpath/..
    done
    echo $relpath
    )
}

ln -s "$(abspath "$scanner")" "$tempdir"/scanner
verify 'launch from abs symlink to abs path' "$tempdir"/scanner -h

ln -s "$(relpath_to_root "$tempdir")$(abspath "$scanner")" "$tempdir"/scanner-rel
verify 'symlink to rel path is valid' test -e "$tempdir"/scanner-rel
verify 'launch from abs symlink to rel path' "$tempdir"/scanner-rel -h

mkdir "$tempdir/x"
ln -s ../scanner "$tempdir"/x/scanner
verify 'symlink to rel symlink is valid' test -f "$tempdir"/x/scanner
verify 'launch from abs symlink that is rel symlink to abs path' "$tempdir"/x/scanner -h
