# encoding: utf-8

from time import time
from uuid import uuid4

from confluent_kafka import SerializingProducer
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroSerializer
from confluent_kafka.serialization import StringSerializer

topic = "wiinvent.gami.event_log"
schema_registry_url = "http://app01:8081"
bootstrap_servers = "app01:9092"

schema_registry_conf = {'url': schema_registry_url}
schema_registry_client = SchemaRegistryClient(schema_registry_conf)

# schema_str = codecs.open("src/main/resources/avro/EventLog.avsc", "r").read()
schema_str = '{"type":"record","name":"EventLog","namespace":"com.wiinvent.gami.avro","fields":[{"name":"id","type":{"type":"string"}},{"name":"type","type":{"type":"string"}},{"name":"userId","type":{"type":"string","avro.java.string":"String"}},{"name":"value","type":"long","default":1},{"name":"occurredAt","type":"long"},{"name":"receivedAt","type":"long"},{"name":"processedAt","type":"long","default":0},{"name":"source","type":{"type":"string","avro.java.string":"String"},"default":""},{"name":"params","type":{"type":"array","items":{"type":"record","name":"EventLogParam","namespace":"com.wiinvent.gami.avro","fields":[{"name":"name","type":{"type":"string","avro.java.string":"String"}},{"name":"value","type":{"type":"string","avro.java.string":"String"}}]}},"default":[]}]}'
avro_serializer = AvroSerializer(schema_registry_client, schema_str)
producer_conf = {'bootstrap.servers': bootstrap_servers,
                 'key.serializer': StringSerializer('utf_8'),
                 'value.serializer': avro_serializer}

producer = SerializingProducer(producer_conf)

for i in range(0, 5):
    print(f"----> {i}")
    value = {
        "id": str(uuid4()),
        "type": "card_bought",
        "userId": "vt_999",
        "value": 1,
        "occurredAt": int(time() * 1000) + 86400000 * (i - 4),
        "receivedAt": int(time() * 1000) + 86400000 * (i - 4),
        "source": "Talk your way",
        # "params": [
        #     {
        #         "name": "package",
        #         "value": "50K"
        #     }
        # ]
    }

    producer.produce(topic=topic, key=value['id'], value=value)
    producer.flush()

