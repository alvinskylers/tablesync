package com.alvinskylers.tablesync;

import com.alvinskylers.tablesync.controller.ReservationController;
import com.alvinskylers.tablesync.controller.TableController;
import com.alvinskylers.tablesync.controller.UserController;
import com.alvinskylers.tablesync.entity.enums.Role;
import com.alvinskylers.tablesync.exception.ReservationNotFoundException;
import com.alvinskylers.tablesync.exception.ReservationUpdateStatusException;
import com.alvinskylers.tablesync.exception.TableReservedException;
import com.alvinskylers.tablesync.security.JwtAuthEntryPoint;
import com.alvinskylers.tablesync.security.JwtService;
import com.alvinskylers.tablesync.security.SecurityConfig;
import com.alvinskylers.tablesync.security.UserPrincipalService;
import com.alvinskylers.tablesync.service.ReservationService;
import com.alvinskylers.tablesync.service.TableService;
import com.alvinskylers.tablesync.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ReservationController.class, TableController.class, UserController.class})
@Import({JwtAuthEntryPoint.class, SecurityConfig.class})
public class RestControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ReservationService reservationService;
    @MockitoBean TableService tableService;
    @MockitoBean UserService userService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserPrincipalService userPrincipalService;
    private static final String RESERVATION_BASE_URL = "/api/v1/reservations";
    private static final String TABLE_BASE_URL = "/api/v1/tables";
    private static final String USERS_BASE_URL = "/api/v1/users";
    private static final String PATH_VARIABLE = "/" + UUID.randomUUID();

    @Test
    void getReservation_noToken_return401() throws Exception {
        mockMvc.perform(get(RESERVATION_BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "WAITER", "CUSTOMER"})
    void postReservation_withEmptyBody_return400(String role) throws Exception {
        mockMvc.perform(post(RESERVATION_BASE_URL)
                        .with(user("wu").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postReservation_asNonStaff_return201() throws Exception {
        mockMvc.perform(post(RESERVATION_BASE_URL)
                        .with(user("c").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationRequestBody()))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"WAITER", "ADMIN"})
    void postReservation_asStaff_return403(String role) throws Exception {
        mockMvc.perform(post(RESERVATION_BASE_URL)
                        .with(user("w").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationRequestBody()))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"WAITER", "ADMIN"})
    void patchReservation_asStaff_return200(String role) throws Exception {
        mockMvc.perform(patch(RESERVATION_BASE_URL + PATH_VARIABLE)
                        .with(user("w").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationStatusRequestBody()))
                .andExpect(status().isOk());
    }

    @Test
    void patchReservation_asNonStaff_return403() throws Exception {
        mockMvc.perform(patch(RESERVATION_BASE_URL + PATH_VARIABLE)
                        .with(user("w").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationStatusRequestBody()))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"WAITER","ADMIN"})
    void deleteReservation_asStaff_return200(String role) throws Exception {
        mockMvc.perform(delete(RESERVATION_BASE_URL + PATH_VARIABLE)
                        .with(user("w").roles(role)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteReservation_asNonStaff_return200() throws Exception {
        mockMvc.perform(delete(RESERVATION_BASE_URL + PATH_VARIABLE)
                        .with(user("c").roles("CUSTOMER")))
                .andExpect(status().isOk());
    }

    @Test
    void postReservation_emptyBodyAsCustomer_returns400WithValidationDetails() throws Exception {
        mockMvc.perform(post(RESERVATION_BASE_URL)
                        .with(user("c").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details").isNotEmpty());
    }

    @Test
    void postReservation_tableAlreadyReserved_returns409() throws Exception {
        when(reservationService.createReservation(any()))
                .thenThrow(new TableReservedException("table is reserved"));

        mockMvc.perform(post(RESERVATION_BASE_URL)
                        .with(user("c").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationRequestBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("TABLE_RESERVED"));
    }

    @Test
    void deleteReservation_notFound_returns404() throws Exception {
        when(reservationService.cancelReservation(any()))
                .thenThrow(new ReservationNotFoundException("reservation not found"));

        mockMvc.perform(delete(RESERVATION_BASE_URL + PATH_VARIABLE)
                        .with(user("c").roles("CUSTOMER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESERVATION_NOT_FOUND"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "WAITER"})
    void patchReservation_invalidStatusTransition_returns409(String role) throws Exception {
        when(reservationService.updateReservation(any(), any()))
                .thenThrow(new ReservationUpdateStatusException("cannot update"));

        mockMvc.perform(patch(RESERVATION_BASE_URL + PATH_VARIABLE)
                        .with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationStatusRequestBody()))
                .andExpect(status().isConflict());
    }


    @Test
    void getTables_asNoUser_return200() throws Exception {
        mockMvc.perform(get(TABLE_BASE_URL))
                .andExpect(status().isOk());
    }

    @Test
    void postTable_asAdmin_return201() throws Exception {
        mockMvc.perform(post(TABLE_BASE_URL)
                        .with(user("a").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tableRequestBody()))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"WAITER", "CUSTOMER"})
    void postTable_asNonAdmin_return403(String role) throws Exception {
        mockMvc.perform(post(TABLE_BASE_URL)
                        .with(user("w").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tableRequestBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void putTable_asAdmin_return200() throws Exception {
        mockMvc.perform(put(TABLE_BASE_URL + PATH_VARIABLE)
                        .with(user("a").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tableRequestBody()))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"WAITER", "CUSTOMER"})
    void putTable_asNonAdmin_return403(String role) throws Exception {
        mockMvc.perform(put(TABLE_BASE_URL + PATH_VARIABLE)
                        .with(user("w").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tableRequestBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteTable_asAdmin_return200() throws Exception {
        mockMvc.perform(delete(TABLE_BASE_URL + PATH_VARIABLE)
                        .with(user("a").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tableRequestBody()))
                .andExpect(status().is2xxSuccessful());
    }

    @ParameterizedTest
    @ValueSource(strings = {"WAITER", "CUSTOMER"})
    void deleteTable_asNonAdmin_return403(String role) throws Exception {
        mockMvc.perform(delete(TABLE_BASE_URL + PATH_VARIABLE)
                        .with(user("w").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tableRequestBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUser_asAdmin_return200() throws Exception {
        mockMvc.perform(get(USERS_BASE_URL)
                .with(user("a").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CUSTOMER", "WAITER"})
    void getUser_asNonAdmin_return403(String role) throws Exception {
        mockMvc.perform(get(USERS_BASE_URL)
                        .with(user("c").roles(role)))
                .andExpect(status().isForbidden());
    }

    @Test
    void postUser_asAdmin_return201() throws Exception {
        mockMvc.perform(post(USERS_BASE_URL)
                        .with(user("a").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userRequestBody()))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"WAITER", "CUSTOMER"})
    void postUser_asNonAdmin_return403(String role) throws Exception {
        mockMvc.perform(post(USERS_BASE_URL)
                        .with(user("w").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userRequestBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void putUser_asAdmin_return200() throws Exception {
        mockMvc.perform(put(USERS_BASE_URL + PATH_VARIABLE)
                        .with(user("a").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userRequestBody()))
                .andExpect(status().is2xxSuccessful());
    }

    @ParameterizedTest
    @ValueSource(strings = {"WAITER", "CUSTOMER"})
    void putUser_asNonAdmin_return403(String role) throws Exception {
        mockMvc.perform(put(USERS_BASE_URL + PATH_VARIABLE)
                        .with(user("w").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userRequestBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_asAdmin_return204() throws Exception {
        mockMvc.perform(delete(USERS_BASE_URL + PATH_VARIABLE)
                        .with(user("a").roles("ADMIN")))
                .andExpect(status().isNoContent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"WAITER", "CUSTOMER"})
    void deleteUser_asNonAdmin_return403(String role) throws Exception {
        mockMvc.perform(delete(USERS_BASE_URL + PATH_VARIABLE)
                        .with(user("w").roles(role)))
                .andExpect(status().isForbidden());
    }

    private String reservationRequestBody() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(Map.of(
                "tableId", UUID.randomUUID().toString(),
                "reservationStart", LocalDateTime.now().plusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "reservationEnd", LocalDateTime.now().plusHours(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        ));
    }

    private String reservationStatusRequestBody() {
        return "{\"status\":\"CONFIRMED\"}";
    }

    private String tableRequestBody() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(Map.of(
           "tableNumber","1",
           "seatCount", "2"
        ));
    }

    private String userRequestBody() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(Map.of(
                "email","test@mail.com",
                "password", "password",
                "phone","0934548358345435",
                "role",Role.CUSTOMER
        ));
    }
}
