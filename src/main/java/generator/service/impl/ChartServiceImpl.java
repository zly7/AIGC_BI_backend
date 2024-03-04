package generator.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import generator.domain.Chart;
import generator.service.ChartService;
import generator.mapper.ChartMapper;
import org.springframework.stereotype.Service;

/**
* @author Asus
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2024-02-27 23:52:46
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{

}




