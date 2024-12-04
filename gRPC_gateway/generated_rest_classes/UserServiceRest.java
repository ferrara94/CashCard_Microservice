
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("userservice")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserServiceRest {

    private final UserServiceGrpc.UserServiceBlockingStub grpcStub;

    public UserServiceRest(UserServiceGrpc.UserServiceBlockingStub grpcStub) {
        this.grpcStub = grpcStub;
    }

    @GET
    @Path("/users/{id}")
    public Object GetUser(@PathParam("id") String id) {
        UserRequest request = UserRequest.newBuilder()
            .setId(id)
            .build();
        return grpcStub.GetUser(request);
    }
    @GET
    @Path("/users/{id}")
    public Object CreateUser(@PathParam("id") String id) {
        CreateUserRequest request = CreateUserRequest.newBuilder()
            .setId(id)
            .build();
        return grpcStub.CreateUser(request);
    }
}
