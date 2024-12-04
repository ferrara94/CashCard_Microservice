const fs = require('fs');
const path = require('path');
const handlebars = require('handlebars');

// Handlebars template for Java REST class
const javaTemplate = `
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("{{servicePath}}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class {{serviceName}}Rest {

    private final {{serviceName}}Grpc.{{serviceName}}BlockingStub grpcStub;

    public {{serviceName}}Rest({{serviceName}}Grpc.{{serviceName}}BlockingStub grpcStub) {
        this.grpcStub = grpcStub;
    }

    {{#each methods}}
    @{{httpMethod}}
    @Path("{{path}}")
    public Object {{name}}({{#if param}}@PathParam("{{param}}") String {{param}}{{else}}Object requestBody{{/if}}) {
        {{#if param}}
        {{requestType}} request = {{requestType}}.newBuilder()
            .set{{capitalize param}}({{param}})
            .build();
        {{else}}
        {{requestType}} request = ({{requestType}}) requestBody;
        {{/if}}
        return grpcStub.{{name}}(request);
    }
    {{/each}}
}
`;

// Capitalize function for Handlebars
handlebars.registerHelper('capitalize', function (str) {
    return str.charAt(0).toUpperCase() + str.slice(1);
});

// Parse the .proto file
function parseProto(filePath) {
    const protoContent = fs.readFileSync(filePath, 'utf-8');
    const lines = protoContent.split('\n');

    const services = [];
    let currentService = null;

    lines.forEach((line) => {
        const serviceMatch = line.match(/^\s*service\s+(\w+)\s*{/);
        if (serviceMatch) {
            currentService = { name: serviceMatch[1], methods: [] };
            services.push(currentService);
        }

        const rpcMatch = line.match(/^\s*rpc\s+(\w+)\s*\((\w+)\)\s+returns\s+\((\w+)\)\s*{/);
        if (rpcMatch && currentService) {
            const method = {
                name: rpcMatch[1],
                requestType: rpcMatch[2],
                responseType: rpcMatch[3],
                httpMethod: 'POST',
                path: '',
                param: null,
            };
            currentService.methods.push(method);
        }

        const httpOptionMatch = line.match(/^\s*option\s+\(google\.api\.http\)\s*=\s*{/);
        if (httpOptionMatch && currentService && currentService.methods.length > 0) {
            const method = currentService.methods[currentService.methods.length - 1];
            const httpDetails = lines
                .slice(lines.indexOf(line) + 1)
                .find((subLine) => subLine.includes('}'));

            const httpMatch = httpDetails.match(/(get|post|put|delete):\s*"([^"]+)"/i);
            if (httpMatch) {
                method.httpMethod = httpMatch[1].toUpperCase();
                method.path = httpMatch[2];
                const paramMatch = method.path.match(/{(\w+)}/);
                if (paramMatch) {
                    method.param = paramMatch[1];
                }
            }
        }
    });

    return services;
}

// Generate Java REST classes
function generateJavaClasses(services, outputDir) {
    if (!fs.existsSync(outputDir)) {
        fs.mkdirSync(outputDir, { recursive: true });
    }

    services.forEach((service) => {
        const fileName = `${service.name}Rest.java`;
        const filePath = path.join(outputDir, fileName);

        const javaCode = handlebars.compile(javaTemplate)({
            serviceName: service.name,
            servicePath: service.name.toLowerCase(),
            methods: service.methods,
        });

        fs.writeFileSync(filePath, javaCode, 'utf-8');
        console.log(`Generated: ${filePath}`);
    });
}

// Main function
function main() {
    const protoFile = 'user.proto'; // Replace with your .proto file path
    const outputDir = 'generated_rest_classes';

    console.log('Parsing .proto file...');
    const services = parseProto(protoFile);
    console.log(`Found ${services.length} services.`);

    console.log('Generating Java REST classes...');
    generateJavaClasses(services, outputDir);
    console.log('Done.');
}

main();
