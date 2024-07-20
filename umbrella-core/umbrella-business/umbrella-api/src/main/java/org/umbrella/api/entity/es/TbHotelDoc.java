package org.umbrella.api.entity.es;

import lombok.Data;
import lombok.ToString;
import org.springframework.beans.BeanUtils;
import org.umbrella.api.entity.TbHotel;

@Data
@ToString
public class TbHotelDoc {
    private String id;
    private String name;
    private String address;
    private Integer price;
    private Integer score;
    private String brand;
    private String city;
    private String starName;
    private String business;
    private String location;
    private String pic;

    public TbHotelDoc() {}

    public TbHotelDoc(TbHotel tbHotel) {
        BeanUtils.copyProperties(tbHotel, this);
        id = tbHotel.getId().toString();
        location = tbHotel.getLatitude() + ", " + tbHotel.getLongitude();
    }
}
