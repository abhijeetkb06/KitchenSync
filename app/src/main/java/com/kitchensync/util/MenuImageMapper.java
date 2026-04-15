package com.kitchensync.util;

import com.kitchensync.R;

import java.util.HashMap;
import java.util.Map;

public final class MenuImageMapper {
    private MenuImageMapper() {}

    private static final Map<String, Integer> IMAGE_MAP = new HashMap<>();

    static {
        IMAGE_MAP.put("crunchy_taco", R.drawable.food_crunchy_taco);
        IMAGE_MAP.put("doritos_taco", R.drawable.food_doritos_taco);
        IMAGE_MAP.put("soft_taco", R.drawable.food_soft_taco);
        IMAGE_MAP.put("chalupa", R.drawable.food_chalupa);
        IMAGE_MAP.put("bean_burrito", R.drawable.food_bean_burrito);
        IMAGE_MAP.put("beefy_5layer", R.drawable.food_beefy_5layer);
        IMAGE_MAP.put("crunchwrap", R.drawable.food_crunchwrap);
        IMAGE_MAP.put("quesarito", R.drawable.food_quesarito);
        IMAGE_MAP.put("chips_nacho", R.drawable.food_chips_nacho);
        IMAGE_MAP.put("cinnamon_twists", R.drawable.food_cinnamon_twists);
        IMAGE_MAP.put("fiesta_potatoes", R.drawable.food_fiesta_potatoes);
        IMAGE_MAP.put("baja_blast", R.drawable.food_baja_blast);
        IMAGE_MAP.put("pepsi", R.drawable.food_pepsi);
        IMAGE_MAP.put("tb_iced_coffee", R.drawable.food_tb_iced_coffee);
    }

    public static int getDrawableRes(String imageKey) {
        if (imageKey == null) return 0;
        Integer resId = IMAGE_MAP.get(imageKey);
        return resId != null ? resId : 0;
    }
}
