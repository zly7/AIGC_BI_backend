package com.yupi.springbootinit.controller;
import java.util.Arrays;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.springbootinit.annotation.AuthCheck;
import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.DeleteRequest;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.common.ResultUtils;
import com.yupi.springbootinit.constant.CommonConstant;
import com.yupi.springbootinit.constant.UserConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.exception.ThrowUtils;
import com.yupi.springbootinit.manager.*;
import com.yupi.springbootinit.model.dto.chart.*;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.model.vo.BiResponse;
import com.yupi.springbootinit.service.ChartService;
import com.yupi.springbootinit.service.UserService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import com.yupi.springbootinit.utils.ExcelUtils;
import com.yupi.springbootinit.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 帖子接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
//@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AiManager aiManager;

    @Resource
    private AIAssistantManager aiAssistantManager;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private OpenAIManager openAIManager;

    @Resource
    private LangChainManager langChainManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<Chart> getChartVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（仅管理员）
     *
     * @param chartQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<Chart>> listChartVOByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<Chart>> listMyChartVOByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion


    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 文件上传
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String chartType = genChartByAiRequest.getChartType();
        String goal = genChartByAiRequest.getGoal();
        String name = genChartByAiRequest.getName();
        String modelName = genChartByAiRequest.getModelName();
        StringBuilder allPrompt = new StringBuilder();
        //必须要登陆才能使用
        User loginInUser = userService.getLoginUser(request);

        long size = multipartFile.getSize();
        String filename = multipartFile.getOriginalFilename();
        final long ONE_MB = 1 * 1024 * 1024L;
        ThrowUtils.throwIf(size>ONE_MB, ErrorCode.PARAMS_ERROR, "上传文件过大");
        String suffix = FileUtil.getSuffix(filename);
        List<String> validateFileSuffix= Arrays.asList("csv","xlsx");
        ThrowUtils.throwIf(!validateFileSuffix.contains(suffix),ErrorCode.PARAMS_ERROR,"上传文件后缀不合法");

        //限流操作,每个用户对应的每个方法的限流器
        redisLimiterManager.doLimit("genChartByAi_"+String.valueOf(loginInUser.getId()));
        allPrompt.append("你是一个数据分析师，现在我会把原始的数据给你，你需要帮我按照要求总结总结。请格式按照要求的【【【【【进行分割，" +
                "也就是要生成两部分，第一部分是生成图表的前端 Echarts V5 的 option 配置对象is代码，第二部分是分析的数据的语言结果，" +
                "合理地将数据进行可视化，不要生成任何多余的内容。两部分开头都用【【【【【进行开头\n。最后要返回的格式是生成内容(此外不要输出任何多余的开头、结尾、注释):\n" +
                "【【【【【\n"+
                "{前端 Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释,不用markdown格式的```包裹}\n" +
                "【【【【【\n" +
                "{明确的数据分析结论、越详细越好，不要生成多余的注释}");
        allPrompt.append("用户要分析的要求是:\n");
        allPrompt.append(goal).append("\n");
        String csvString = ExcelUtils.excelToCsv(multipartFile);
        allPrompt.append("最后的要生成的图表类型是:\n");
        allPrompt.append(chartType);
        allPrompt.append("原始数据是，这部分是Csv格式，逗号分隔的:\n");
        allPrompt.append(csvString);
        //调用AI
        String answerByAi;
        if(modelName.contains("gpt")){//这个地方应该是Http包请求有这个
            answerByAi = openAIManager.doChat(modelName,allPrompt.toString());
        } else if (modelName.contains("yucongming")) {
            answerByAi = aiManager.doChat(1654785040361893889L, allPrompt.toString());
        }else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"模型名称设置有误");
        }
        answerByAi = answerByAi.replaceAll("】{5}", "");
        String[] splitAnswers = answerByAi.split("【【【【【");
        if(splitAnswers.length!=3){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI 生成错误");
        }
        BiResponse biResponse = new BiResponse();
        String genChart = splitAnswers[1].trim();
        String genResult = splitAnswers[2].trim();
        biResponse.setGenResult(genResult);
        biResponse.setGenChart(genChart);
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setName(name);
        chart.setChartData(csvString);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginInUser.getId());
        chart.setStatus("succeed");
        boolean saveChartResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveChartResult,ErrorCode.SYSTEM_ERROR,"chart保存错误");
        return ResultUtils.success(biResponse);
    }

    /**
     * 文件上传
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async")
    public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                             GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String chartType = genChartByAiRequest.getChartType();
        String goal = genChartByAiRequest.getGoal();
        String name = genChartByAiRequest.getName();
        String modelName = genChartByAiRequest.getModelName();
        StringBuilder allPrompt = new StringBuilder();
        //必须要登陆才能使用
        User loginInUser = userService.getLoginUser(request);

        long size = multipartFile.getSize();
        String filename = multipartFile.getOriginalFilename();
        final long ONE_MB = 1 * 1024 * 1024L;
        ThrowUtils.throwIf(size>ONE_MB, ErrorCode.PARAMS_ERROR, "上传文件过大");
        String suffix = FileUtil.getSuffix(filename);
        List<String> validateFileSuffix= Arrays.asList("csv","xlsx");
        ThrowUtils.throwIf(!validateFileSuffix.contains(suffix),ErrorCode.PARAMS_ERROR,"上传文件后缀不合法");

        //限流操作,每个用户对应的每个方法的限流器
        redisLimiterManager.doLimit("genChartByAi_"+String.valueOf(loginInUser.getId()));
        allPrompt.append("你是一个数据分析师，现在我会把原始的数据给你，你需要帮我按照要求总结总结。请格式按照要求的【【【【【进行分割，" +
                "也就是要生成两部分，第一部分是生成图表的前端 Echarts V5 的 option 配置对象is代码，第二部分是分析的数据的语言结果，" +
                "合理地将数据进行可视化，不要生成任何多余的内容。两部分开头都用【【【【【进行开头\n。最后要返回的格式是生成内容(此外不要输出任何多余的开头、结尾、注释):\n" +
                "【【【【【\n"+
                "{前端 Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化，Echart JSON 所有的每个字符串一定要放双引号，把所有单引号都变成双引号，不要生成任何多余的内容，比如注释,不用markdown格式的```包裹}\n" +
                "【【【【【\n" +
                "{明确的数据分析结论、越详细越好，不要生成多余的注释}");
        allPrompt.append("用户要分析的要求是:\n");
        allPrompt.append(goal).append("\n");
        String csvString = ExcelUtils.excelToCsv(multipartFile);
        allPrompt.append("最后的要生成的图表类型是:\n");
        allPrompt.append(chartType);
        allPrompt.append("原始数据是，这部分是Csv格式，逗号分隔的:\n");
        allPrompt.append(csvString);

        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setName(name);
        chart.setChartData(csvString);
        chart.setChartType(chartType);
        chart.setUserId(loginInUser.getId());
        chart.setStatus("wait");
        boolean saveChartResult = chartService.save(chart);
        if (!saveChartResult){
            handleUpdatedError(chart.getId(),"chart保存错误");
        }
        CompletableFuture.runAsync(() -> {
            Chart updatedChart = chartService.getById(chart.getId());
            updatedChart.setStatus("running");
            String answerByAi;
            if(modelName.contains("gpt")){
                answerByAi = openAIManager.doChat(modelName,allPrompt.toString());
            } else if (modelName.contains("yucongming")) {
                answerByAi = aiManager.doChat(1654785040361893889L, allPrompt.toString());
            }else {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"模型名称设置有误");
            }
            answerByAi = answerByAi.replaceAll("】{5}", "");
            String[] splitAnswers = answerByAi.split("【【【【【");
            if(splitAnswers.length!=3){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI 生成错误");
            }

            String genChart = splitAnswers[1].trim();
            String genResult = splitAnswers[2].trim();
            updatedChart.setGenResult(genResult);
            updatedChart.setGenChart(genChart);
            updatedChart.setStatus("succeed");
            boolean updatedResult = chartService.updateById(updatedChart);
            if(!updatedResult){
                handleUpdatedError(updatedChart.getId(),"获得AI数据后，更新图表错误");
            }
        },threadPoolExecutor);

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }

    @PostMapping("/gen/aiAssistant")
    public BaseResponse<BiResponse> genChartByAiAssistant(@RequestParam("file") MultipartFile multipartFile,
                                                        GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {


        String chartType = genChartByAiRequest.getChartType();
        String goal = genChartByAiRequest.getGoal();
        String name = genChartByAiRequest.getName();
        String modelName = genChartByAiRequest.getModelName();

        User loginInUser = userService.getLoginUser(request);

        StringBuilder allPrompt = new StringBuilder();
        allPrompt.append("You are a data analyst. Now I will give you the raw data, and you need to help me summarize it according to the requirements. Please format it according to the requirement ##### for separation, which means generating two parts. The first part is the code for the front-end Echarts V5 option configuration object for generating charts, and the second part is the language result of data analysis. Reasonably visualize the data, do not generate any extra content. Both parts start with #####. The format to be returned is the generated content (do not output any extra beginnings, endings, comments):\n" +
                "#####\n" +
                "{Front-end Echarts V5 option configuration object js code, reasonably visualize the data, remember to attach quotes to each string in Echart JSON format, do not generate any extra content such as comments, do not use markdown format ```}\n" +
                "#####\n" +
                "{Clear data analysis conclusions, the more detailed the better,emember to attach quotes to each string in Echart JSON format ,do not generate extra comments}");

        allPrompt.append("The user's analysis requirements are:\n");
        allPrompt.append(goal).append("\n");
        allPrompt.append("The final chart types to be generated are:\n");
        allPrompt.append(chartType);

        try {
            String manager_id = aiAssistantManager.createAssistant("Visualizer", modelName, allPrompt.toString(),multipartFile );

            long size = multipartFile.getSize();
            String filename = multipartFile.getOriginalFilename();
            final long ONE_MB = 1 * 1024 * 1024L;
            ThrowUtils.throwIf(size>ONE_MB, ErrorCode.PARAMS_ERROR, "上传文件过大");
            String suffix = FileUtil.getSuffix(filename);
            List<String> validateFileSuffix= Arrays.asList("csv","xlsx");
            ThrowUtils.throwIf(!validateFileSuffix.contains(suffix),ErrorCode.PARAMS_ERROR,"上传文件后缀不合法");

            //限流操作,每个用户对应的每个方法的限流器
            redisLimiterManager.doLimit("genChartByAi_"+String.valueOf(loginInUser.getId()));
//            String csvString = ExcelUtils.excelToCsv(multipartFile);

            Chart chart = new Chart();
            chart.setGoal(goal);
            chart.setName(name);
//            chart.setChartData(csvString);
            chart.setChartType(chartType);
            chart.setUserId(loginInUser.getId());

            //先从OpenAI 得出一个Assistant ID,在设入进Object里
            chart.setAssistantID(manager_id);

            String threadId = aiAssistantManager.createThread();
            chart.setThreadID(threadId);
            boolean saveChartResult = chartService.save(chart);
            if (!saveChartResult){
                handleUpdatedError(chart.getId(),"chart保存错误");
            }
            String response = aiAssistantManager.sendMessageToThread(allPrompt.toString(), manager_id, threadId);

            String[] splitAnswers = response.split("#####");
            if(splitAnswers.length!=3){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI 生成错误");
            }

            String genChart = splitAnswers[1].trim();
            String genResult = splitAnswers[2].trim();
            Chart updatedChart = chartService.getById(chart.getId());
            updatedChart.setGenResult(genResult);
            updatedChart.setGenChart(genChart);
            updatedChart.setStatus("succeed");
            boolean updatedResult = chartService.updateById(updatedChart);
            if(!updatedResult){
                handleUpdatedError(updatedChart.getId(),"获得AI数据后，更新图表错误");
            }

            BiResponse biResponse = new BiResponse();
            biResponse.setChartId(chart.getId());
            return ResultUtils.success(biResponse);

        }catch(Exception e){
            log.info("createAssistant exception A e: {}", e.getMessage());

            return null;
        }

    }

    @PostMapping("/genLc/async")
    public BaseResponse<BiResponse> genChartByLcAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String chartType = genChartByAiRequest.getChartType();
        String goal = genChartByAiRequest.getGoal();
        String name = genChartByAiRequest.getName();
        String modelName = genChartByAiRequest.getModelName();

        //必须要登陆才能使用
        User loginInUser = userService.getLoginUser(request);

        long size = multipartFile.getSize();
        String filename = multipartFile.getOriginalFilename();
        final long ONE_MB = 1 * 1024 * 1024L;
        ThrowUtils.throwIf(size>ONE_MB, ErrorCode.PARAMS_ERROR, "上传文件过大");
        String suffix = FileUtil.getSuffix(filename);
        List<String> validateFileSuffix= Arrays.asList("csv","xlsx");
        ThrowUtils.throwIf(!validateFileSuffix.contains(suffix),ErrorCode.PARAMS_ERROR,"上传文件后缀不合法");

        //限流操作,每个用户对应的每个方法的限流器
        redisLimiterManager.doLimit("genChartByAi_"+String.valueOf(loginInUser.getId()));

        GiveLangChainManagerDataPackage giveLangChainManagerDataPackage = new GiveLangChainManagerDataPackage();
        giveLangChainManagerDataPackage.setChartType(chartType);
        giveLangChainManagerDataPackage.setGoal(goal);
        String csvString = ExcelUtils.excelToCsv(multipartFile);
        giveLangChainManagerDataPackage.setCsvString(csvString);
        giveLangChainManagerDataPackage.setModelName(modelName);

        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setName(name);
        chart.setChartData(csvString);
        chart.setChartType(chartType);
        chart.setUserId(loginInUser.getId());
        chart.setStatus("wait");
        boolean saveChartResult = chartService.save(chart);
        if (!saveChartResult){
            handleUpdatedError(chart.getId(),"chart保存错误");
        }
        CompletableFuture.runAsync(() -> {
            Chart updatedChart = chartService.getById(chart.getId());
            updatedChart.setStatus("running");
            String answerByAi;
            if(modelName.contains("gpt")){
                answerByAi = langChainManager.doLcChat(giveLangChainManagerDataPackage);
            } else if (modelName.contains("yucongming")) {
                StringBuilder allPrompt = new StringBuilder();
                allPrompt.append("你是一个数据分析师，现在我会把原始的数据给你，你需要帮我按照要求总结总结。请格式按照要求的#####进行分割，" +
                        "也就是要生成两部分，第一部分是生成图表的前端 Echarts V5 的 option 配置对象is代码，第二部分是分析的数据的语言结果，" +
                        "合理地将数据进行可视化，不要生成任何多余的内容。两部分开头都用#####进行开头\n。最后要返回的格式是生成内容(此外不要输出任何多余的开头、结尾、注释):\n" +
                        "#####\n"+
                        "{前端 Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化，Echart JSON 所有的每个字符串一定要放双引号，把所有单引号都变成双引号，不要生成任何多余的内容，比如注释,不用markdown格式的```包裹}\n" +
                        "#####\n" +
                        "{明确的数据分析结论、越详细越好，不要生成多余的注释}");
                allPrompt.append("用户要分析的要求是:\n");
                allPrompt.append(goal).append("\n");
                allPrompt.append("最后的要生成的图表类型是:\n");
                allPrompt.append(chartType);
                allPrompt.append("原始数据是，这部分是Csv格式，逗号分隔的:\n");
                allPrompt.append(csvString);
                answerByAi = aiManager.doChat(1654785040361893889L, allPrompt.toString());
            }else {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"模型名称设置有误");
            }
            String[] splitAnswers = answerByAi.split("#####");
            if(splitAnswers.length!=3){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI 生成错误");
            }

            String genChart = splitAnswers[1].trim();
            String genResult = splitAnswers[2].trim();
            updatedChart.setGenResult(genResult);
            updatedChart.setGenChart(genChart);
            updatedChart.setStatus("succeed");
            boolean updatedResult = chartService.updateById(updatedChart);
            if(!updatedResult){
                handleUpdatedError(updatedChart.getId(),"获得AI数据后，更新图表错误");
            }
        },threadPoolExecutor);

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }
    private void handleUpdatedError(Long chartId,String execMessage){
        Chart newChart = new Chart();
        newChart.setId(chartId);
        newChart.setStatus("failed");
        newChart.setExecMessage(execMessage);
        boolean updatedResult = chartService.updateById(newChart);
        return;
    }
    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */
    public QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();

        String name = chartQueryRequest.getName();

        String chartType = chartQueryRequest.getChartType();
        String goal = chartQueryRequest.getGoal();
        Long userId = chartQueryRequest.getUserId();
        queryWrapper.eq(id != null && id > 0,"id",id);

        queryWrapper.like(StringUtils.isNotBlank(name),"name", name);

        queryWrapper.eq(StringUtils.isNotBlank(chartType),"chartType",chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId),"userId",userId);
        queryWrapper.eq(StringUtils.isNotBlank(goal),"goal",goal);
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }


}
