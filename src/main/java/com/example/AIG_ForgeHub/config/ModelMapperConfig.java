package com.example.AIG_ForgeHub.config;

import com.example.AIG_ForgeHub.dto.dashboardDto.RFQItemResponseDto;
import com.example.AIG_ForgeHub.dto.dashboardDto.RFQResponseDto;
import com.example.AIG_ForgeHub.dto.dashboardDto.UserResponseDto;
import com.example.AIG_ForgeHub.entity.RFQ;
import com.example.AIG_ForgeHub.entity.RFQItem;
import com.example.AIG_ForgeHub.entity.User;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper=new ModelMapper();


        /*TypeMap<User,UserResponseDto> userMap=
                modelMapper.createTypeMap(User.class,UserResponseDto.class);

        userMap.addMapping(User::getUserId,UserResponseDto::setUserId);
        userMap.addMapping(User::getFullName,UserResponseDto::setFullName);
        userMap.addMapping(User::getEmail,UserResponseDto::setEmail);
        userMap.addMapping(User::getRole,UserResponseDto::setRole);

        userMap.setPostConverter(context->{
            User source=context.getSource();
            UserResponseDto destination=context.getDestination();

            destination.setFirstTimeLogin(
                    Boolean.TRUE.equals(source.getIsFirstTimeLogin())
            );

            destination.setTotpConfigured(
                    source.getSecretKey()!=null && !source.getSecretKey().isBlank()
            );

            return destination;
        });

        modelMapper.createTypeMap(RFQItem.class,RFQItemResponseDto.class);

        Converter<List<RFQItem>,List<RFQItemResponseDto>> rfqItemsConverter=context->
                context.getSource()==null
                        ? Collections.emptyList()
                        : context.getSource().stream()
                        .map(item->modelMapper.map(item,RFQItemResponseDto.class))
                        .toList();

        TypeMap<RFQ,RFQResponseDto> rfqMap=
                modelMapper.createTypeMap(RFQ.class,RFQResponseDto.class);

        rfqMap.addMappings(mapper->mapper
                .using(rfqItemsConverter)
                .map(RFQ::getItems,RFQResponseDto::setItems));

        rfqMap.setPostConverter(context->{
            RFQ source=context.getSource();
            RFQResponseDto destination=context.getDestination();

            destination.setDeleted(
                    Boolean.TRUE.equals(source.getIsDeleted())
            );

            return destination;
        });*/

        return modelMapper;
    }
}