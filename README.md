# uvmlogfilter
A Java program to filter UVM logs

## Purpose
The goal of this program is to perform filtering of UVM logs to extract only informations that may be interesting to the user.
The whole log is divided in records. The first one starts at the first line recognized as a UVM_* message, and ends at the last line before the next UVM_* message (or at the end of the file).
Currently logs can be filtered based on:

* Severity
* Id
* Time
* Component
* Hierarchy
* Text (in the entire record except the first line)

Basic filters like the ones described above can be combined using logical operators (AND,OR,NOT) to produce more complex filters.
The result of filtering can be saved in a text file.

## Usage

