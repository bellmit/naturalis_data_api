{
        "index": {
                "number_of_shards": 1,
                "number_of_replicas": 0
        },
        "analysis": {
                "analyzer": {
                        "nda_ngram_analyzer": {
                                "tokenizer": "nda_ngram_tokenizer",
                                "filter": "lowercase"
                        }
                },
                "tokenizer": {
                        "nda_ngram_tokenizer": {
                                "type": "nGram",
                                "min_gram": "3",
                                "max_gram": "10",
                                "token_chars": ["letter",
                                "digit"]
                        }
                }
        },
        "mappings": {
                "_default_": {
                        "_all": {
                                "enabled": false
                        }
                }
        }
}
