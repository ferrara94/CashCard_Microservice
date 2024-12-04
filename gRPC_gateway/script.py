import os
import re
from jinja2 import Environment, Template

# Define Jinja2 template for Java REST class
TEMPLATE = """
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("{{ service_path }}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class {{ service_name }}Rest {

    private final {{ service_name }}Grpc.{{ service_name }}BlockingStub grpcStub;

    public {{ service_name }}Rest({{ service_name }}Grpc.{{ service_name }}BlockingStub grpcStub) {
        this.grpcStub = grpcStub;
    }

    {% for method in methods %}
    @{{ method.http_method }}
    @Path("{{ method.path }}")
    public Object {{ method.name }}({% if method.param %}@PathParam("{{ method.param }}") String {{ method.param }}{% else %}Object requestBody{% endif %}) {
        {% if method.param %}
        // Create gRPC request
        {{ method.request_type }} request = {{ method.request_type }}.newBuilder()
                .set{{ method.param | capitalize }}({{ method.param }})
                .build();
        {% else %}
        {{ method.request_type }} request = ({{ method.request_type }}) requestBody;
        {% endif %}
        return grpcStub.{{ method.name }}(request);
    }
    {% endfor %}
}
"""

# Helper to capitalize strings
def capitalize(s):
    return s[0].upper() + s[1:]

def parse_proto(file_path):
    """Parse the .proto file and extract service information."""
    service_definitions = []
    
    with open(file_path, 'r') as f:
        lines = f.readlines()
    
    current_service = None
    for line in lines:
        # Detect service name
        service_match = re.match(r'\s*service\s+(\w+)\s*{', line)
        if service_match:
            current_service = {
                "name": service_match.group(1),
                "methods": []
            }
            service_definitions.append(current_service)
        
        # Detect RPC methods
        rpc_match = re.match(r'\s*rpc\s+(\w+)\s*\((\w+)\)\s+returns\s+\((\w+)\)\s*{', line)
        if rpc_match and current_service:
            current_method = {
                "name": rpc_match.group(1),
                "request_type": rpc_match.group(2),
                "response_type": rpc_match.group(3),
                "http_method": "POST",  # Default to POST unless overwritten
                "path": "",
                "param": None
            }
            current_service["methods"].append(current_method)
        
        # Detect HTTP options
        http_option_match = re.match(r'\s*option\s+\(google\.api\.http\)\s*=\s*{', line)
        if http_option_match and current_service and current_service["methods"]:
            method = current_service["methods"][-1]  # Get the last added method
            
            # Parse the HTTP method and path
            for sub_line in lines[lines.index(line)+1:]:
                if "}" in sub_line:
                    break  # End of option block
                http_match = re.match(r'\s*(get|post|put|delete):\s*\"([^\"]+)\"', sub_line)
                if http_match:
                    method["http_method"] = http_match.group(1).upper()
                    method["path"] = http_match.group(2)
                    
                    # Extract PathParam if present in the path
                    path_param_match = re.search(r'{(\w+)}', method["path"])
                    if path_param_match:
                        method["param"] = path_param_match.group(1)
    return service_definitions

def generate_java_classes(services, output_dir):
    """Generate Java REST classes from service definitions."""
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    
    # Create a Jinja2 Environment and add the capitalize filter
    env = Environment()
    env.filters['capitalize'] = capitalize
    
    # Render classes
    for service in services:
        service_name = service["name"]
        file_name = f"{service_name}Rest.java"
        file_path = os.path.join(output_dir, file_name)
        
        # Render the template
        java_code = env.from_string(TEMPLATE).render(
            service_name=service_name,
            service_path=service_name.lower(),
            methods=service["methods"]
        )
        
        # Write the Java file
        with open(file_path, "w") as f:
            f.write(java_code)
        print(f"Generated: {file_path}")

# Main function
if __name__ == "__main__":
    proto_file = "user.proto"  # Replace with your .proto file path
    output_directory = "generated_rest_classes"

    print("Parsing .proto file...")
    services = parse_proto(proto_file)
    print(f"Found {len(services)} services.")

    print("Generating Java REST classes...")
    generate_java_classes(services, output_directory)
    print("Done.")
