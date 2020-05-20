import json
import re
import sys

# cm_definition = str(sys.argv[0])
# definitions = str(sys.argv[1])
# output = str(sys.argv[2])

# Class defination for queues
class Queue:
    def __init__(self, arguments, auto_delete, durable, name, type, vhost):
        self.arguments = arguments
        self.auto_delete = auto_delete
        self.durable = durable
        self.name = name
        self.type = type
        self.vhost = vhost

# Class defination for Binding
class Binding:
    def __init__(self, destination, destination_type, routing_key , source, arguments, vhost):
        self.arguments = arguments
        self.destination = destination
        self.destination_type = destination_type
        self.routing_key = routing_key
        self.source = source
        self.vhost = vhost

# Class defination for exchange
class Exchange:
    def __init__(self, name, vhost, arguments, type, durable, internal, auto_delete):
        self.name = name
        self.vhost = vhost
        self.arguments = arguments
        self.type = type
        self.durable = durable
        self.internal = internal
        self.auto_delete = auto_delete

# Open the existing json file
with open('./rabbitmq/definitions.json') as f:
    customDefination = json.load(f)

# Open the existing json file for ConsentManager
with open('./rabbitmq/cm_rabbitmq.json') as f:
    cmRabbitMqQueues = json.load(f)

# Creating exchange
for newExchange in cmRabbitMqQueues["exchanges"]:
    if not(re.search('"name": "%s"' % newExchange["name"], json.dumps(customDefination["exchanges"]), re.M)):
        exchange = Exchange(newExchange["name"], "/", newExchange["arguments"], newExchange["type"],
                            True, False, False)
        customDefination["exchanges"].append(exchange.__dict__)
        print("CREATING NEW EXCHANGE: ", exchange.__dict__)

# Merging data from cm config to defination
for newQueue in cmRabbitMqQueues["queues"]:
    if not(re.search('"name": "%s"' % newQueue["name"], json.dumps(customDefination["queues"]), re.M)):
        queue = Queue( newQueue["arguments"],
                       False,
                       True,
                       newQueue["name"],
                       "classic",
                       "/")
        customDefination["queues"].append(queue.__dict__)
        print("CREATING NEW QUEUE: ", queue.__dict__)

        queueBinding = Binding(newQueue["name"],
                               "queue",
                               newQueue["routing_key"],
                               newQueue["exchange"],
                               {},
                               "/")
        customDefination["bindings"].append(queueBinding.__dict__)
        print("CREATING NEW BINDING: ", queueBinding.__dict__)

# Writing back to the defination file
with open('./rabbitmq/definitions.json', 'w') as f:
    json.dump(customDefination, f)