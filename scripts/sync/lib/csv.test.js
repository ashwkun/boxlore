'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const { parseCSVLine, parseCSVRecords } = require('./csv');

describe('parseCSVLine', () => {
    it('splits plain fields', () => {
        assert.deepEqual(parseCSVLine('a,b,c'), ['a', 'b', 'c']);
    });

    it('keeps commas inside quotes', () => {
        assert.deepEqual(parseCSVLine('1,"hello, world",3'), ['1', 'hello, world', '3']);
    });

    it('unescapes doubled quotes', () => {
        assert.deepEqual(parseCSVLine('"say ""hi"""'), ['say "hi"']);
    });
});

describe('parseCSVRecords', () => {
    it('splits on newlines outside quotes', () => {
        assert.deepEqual(
            parseCSVRecords('h1,h2\nr1,r2\nr3,r4\n'),
            ['h1,h2', 'r1,r2', 'r3,r4']
        );
    });

    it('keeps quoted newlines inside one record', () => {
        const records = parseCSVRecords('id,author\n1,"Line one\nLine two",ok\n');
        assert.equal(records.length, 2);
        assert.deepEqual(parseCSVLine(records[1]), ['1', 'Line one\nLine two', 'ok']);
    });

    it('handles CRLF record separators', () => {
        assert.deepEqual(
            parseCSVRecords('a,b\r\nc,d\r\n'),
            ['a,b', 'c,d']
        );
    });

    it('surfaces short rows from unquoted mid-field newlines (export must flatten)', () => {
        // PI dump can emit unquoted author with a raw LF; record split then
        // yields a 4-field fragment — importer must skip those, not bind them.
        const records = parseCSVRecords('id,itunes_id,title,author,description\n1,2,Show,Auth\nor Name,Desc\n');
        assert.equal(records.length, 3);
        assert.equal(parseCSVLine(records[1]).length, 4);
        assert.equal(parseCSVLine(records[2]).length, 2);
    });
});
