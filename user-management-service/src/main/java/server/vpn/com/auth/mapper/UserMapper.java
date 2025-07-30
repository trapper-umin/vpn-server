package server.vpn.com.auth.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import server.vpn.com.auth.dto.RegisterRequest;
import server.vpn.com.auth.dto.UserProfileResponse;
import server.vpn.com.auth.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    User toEntity(RegisterRequest registerRequest);

    UserProfileResponse toProfileResponse(User user);
}