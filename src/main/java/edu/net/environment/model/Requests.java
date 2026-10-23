package edu.net.environment.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.*;

public final class Requests {
    private Requests() {}
    public record Login(@NotBlank String role, @NotBlank @Size(max=32) String account,
                        @NotBlank @Size(max=72) String password) {}
    public record Register(@Pattern(regexp="1[3-9][0-9]{9}", message="手机号格式不正确") @NotBlank String phone,
                           @NotBlank @Size(max=40) String name, @Past LocalDate birthday,
                           @Min(0) @Max(1) int sex,
                           @NotBlank @Size(min=6,max=32) String password) {}
    public record Feedback(@NotNull Integer provinceId, @NotNull Integer cityId,
                           @NotBlank @Size(max=200) String address,
                           @NotBlank @Size(max=400) String information,
                           @Min(1) @Max(6) int estimatedGrade) {}
    public record Assignment(@NotNull Long gridMemberId) {}
    public record Measurement(
        @NotNull @DecimalMin("0") @DecimalMax("9999999.999") @Digits(integer=7,fraction=3) BigDecimal so2,
        @NotNull @DecimalMin("0") @DecimalMax("9999999.999") @Digits(integer=7,fraction=3) BigDecimal co,
        @NotNull @DecimalMin("0") @DecimalMax("9999999.999") @Digits(integer=7,fraction=3) BigDecimal pm25) {}
    public record GridMember(@NotBlank @Pattern(regexp="[a-zA-Z0-9_]{3,32}") String account,
        @NotBlank @Size(max=40) String name, @NotBlank @Pattern(regexp="1[3-9][0-9]{9}") String phone,
        @NotNull Integer provinceId, @NotNull Integer cityId, boolean enabled) {}
    public record Supervisor(@NotBlank @Size(max=40) String name, @Past LocalDate birthday,
                             @Min(0) @Max(1) int sex, boolean enabled) {}
    public record Password(@NotBlank @Size(min=6,max=32) String password) {}
}
