set -e
rabbitmqctl wait /var/lib/rabbitmq/mnesia/rabbit@$HOSTNAME.pid
# Adding virtual hosts
rabbitmqctl add_vhost tenant1
rabbitmqctl add_vhost tenant2
# Setting permissions for user 'root'
rabbitmqctl set_permissions -p tenant1 guest ".*" ".*" ".*"
rabbitmqctl set_permissions -p tenant2 guest ".*" ".*" ".*"
# Confirmation message
echo "Virtual hosts have been configured"
