package com.yss.datamiddle.semantic.synonym.exception;

import com.yss.datamiddle.semantic.term.exception.StateConflictException;

public class SynonymConceptConflictException extends StateConflictException {
    public SynonymConceptConflictException(String concept) {
        super("SYNONYM_CONCEPT_CONFLICT: 同义词组名或主词已存在: " + concept);
    }
}
