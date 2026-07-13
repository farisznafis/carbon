import { forwardRef, useEffect, useImperativeHandle, useRef } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { z } from "zod";
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from "react-hook-form";
import { Form, FormItem, FormField, FormLabel, FormControl, FormMessage } from "@/components/ui/form";
import { Checkbox } from "@/components/ui/checkbox";

const FinalTermsSchema = z.object({
  finalTermsAgreed: z.boolean().refine(val => val === true, "蜷梧э縺励※縺上□縺輔＞"),
})

type FinalTermsData = z.infer<typeof FinalTermsSchema>;

export interface FinalTermsRef {
  triggerValidation: () => Promise<boolean>;
}

export type FinalTermsValues = {
  finalTermsAgreed: boolean;
}

interface FinalTermsStepProps {
  formValues: Partial<FinalTermsValues>;
  onValueChange: (values: Partial<FinalTermsData>) => void;
  errors?: Record<string, string>;
}

export const FinalTermsStep = forwardRef<FinalTermsRef, FinalTermsStepProps>(({
  formValues,
  onValueChange,
}, ref) => {

  const form = useForm<FinalTermsData>({
      // resolver: zodResolver(FinalTermsSchema),
      mode: "onChange",
      defaultValues: {
        finalTermsAgreed: false,
      },
      ...(formValues as Partial<FinalTermsData>),
    });
    const watchedValues = form.watch();
  
    useImperativeHandle(ref, () => ({
      triggerValidation: async () => {
        return await form.trigger(); // parent can call this to validate
      },
    }), [form]);
  
    // Reset form with saved values when component mounts or formValues changes
    useEffect(() => {
      form.reset(formValues as FinalTermsValues
      );
    }, [form, formValues]);
  
    const prevValuesRef = useRef(watchedValues);
  
    useEffect(() => {
      if (JSON.stringify(prevValuesRef.current) !== JSON.stringify(watchedValues)) {
        prevValuesRef.current = watchedValues;
        onValueChange(watchedValues);
      }
    }, [watchedValues, onValueChange]);

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold mb-2">「プログラム名」入会規約 <span className="ml-1 text-destructive">*</span></h2>
        <p className="text-muted-foreground">入会規約をご確認の上、同意してください</p>
      </div>

      <Card>
        <CardContent className="pt-6">
          <div className="max-h-[600px] overflow-y-auto p-6 border rounded-lg bg-muted/30">
            <div className="text-sm leading-relaxed space-y-6">
              <section>
                <h3 className="font-semibold mb-2">(蜷咲ｧｰ)</h3>
                <h4 className="font-semibold mb-2">隨ｬ1譚｡</h4>
                <p>譛ｬ莨壹・蜷咲ｧｰ縺ｯ縲√・繝ｭ繧ｰ繝ｩ繝蜷・(莉･荳九梧悽莨壹阪→縺・≧) 縺ｨ縺吶ｋ縲・</p>
              </section>

              <section>
                <h3 className="font-semibold mb-2">(逶ｮ逧・</h3>
                <h4 className="font-semibold mb-2">隨ｬ2譚｡</h4>
                <p>譛ｬ莨壹・縲√・繝ｭ繧ｰ繝ｩ繝蝙矩°蝟ｶ繝ｻ邂｡逅・・錐縺悟ｮ滓命縺吶ｋ縲後・繝ｭ繧ｸ繧ｧ繧ｯ繝域ｦりｦ√阪・荳迺ｰ縺ｨ縺励※縲∽ｼ壼藤縺悟､ｪ髯ｽ蜈臥匱髮ｻ險ｭ蛯吶ｒ菴ｿ逕ｨ縺吶ｋ縺薙→縺ｧ蜑頑ｸ帙＆繧後◆貂ｩ螳､蜉ｹ譫懊ぎ繧ｹ謗貞・驥上ｒ縲∝嵜蜀・↓縺翫￠繧句慍逅・ｸｩ證門喧蟇ｾ遲悶・縺溘ａ縺ｮ謗貞・蜑頑ｸ帙・蜷ｸ蜿朱㍼隱崎ｨｼ蛻ｶ蠎ｦ (莉･荳九鯉ｼｪ・阪け繝ｬ繧ｸ繝・ヨ蛻ｶ蠎ｦ縲阪→縺・≧) 螳滓命隕∫ｶｱ (蟷ｳ謌・5蟷ｴ4譛・7譌･邨梧ｸ育肇讌ｭ逵√∫腸蠅・怐縲∬ｾｲ譫玲ｰｴ逕｣逵∫ｭ門ｮ・ 縺ｫ蝓ｺ縺･縺搾ｼｪ・阪け繝ｬ繧ｸ繝・ヨ縺ｨ縺励※・ｪ・阪け繝ｬ繧ｸ繝・ヨ蛻ｶ蠎ｦ隱崎ｨｼ蟋泌藤莨・(・ｪ・阪け繝ｬ繧ｸ繝・ヨ蛻ｶ蠎ｦ繧堤ｮ｡逅・☆繧狗ｵ梧ｸ育肇讌ｭ逵√∫腸蠅・怐縲∬ｾｲ譫玲ｰｴ逕｣逵√′蜈ｱ蜷後〒險ｭ鄂ｮ縺励◆縲・ｼｪ・阪け繝ｬ繧ｸ繝・ヨ縺ｮ隱崎ｨｼ繧定｡後≧蟋泌藤莨・ 繧医ｊ隱崎ｨｼ繧貞女縺代ｋ縺薙→縺ｧ縲∫腸蠅・ｾ｡蛟､縺ｮ縲瑚ｦ九∴繧句喧縲阪ｒ蝗ｳ繧九→縺ｨ繧ゅ↓縲∝卸蜃ｺ縺輔ｌ縺滂ｼｪ・阪け繝ｬ繧ｸ繝・ヨ繧偵・繝ｭ繧ｰ繝ｩ繝蝙矩°蝟ｶ繝ｻ邂｡逅・・錐縺ｫ縺翫￠繧句茜逕ｨ逶ｮ逧・↓蛻ｩ逕ｨ縺吶ｋ縺薙→繧堤岼逧・→縺吶ｋ縲・</p>
              </section>

              <section>
                <h3 className="font-semibold mb-2">(驕句霧繝ｻ邂｡逅・</h3>
                <h4 className="font-semibold mb-2">隨ｬ3譚｡</h4>
                <p>譛ｬ莨壹・驕句霧繝ｻ邂｡逅・・繝励Ο繧ｰ繝ｩ繝蝙矩°蝟ｶ繝ｻ邂｡逅・・錐 (莉･荳九碁°蝟ｶ繝ｻ邂｡逅・・阪→縺・≧) 縺瑚｡後≧縲・</p>
              </section>

              <section>
                <h4 className="font-semibold mb-2">隨ｬ3譚｡縺ｮ2</h4>
                <p className="mb-2">驕句霧繝ｻ邂｡逅・・・・ｪ・阪け繝ｬ繧ｸ繝・ヨ蛻ｶ蠎ｦ縺ｫ髢｢縺励∵ｬ｡縺ｫ謗ｲ縺偵ｋ讌ｭ蜍吶ｒ陦後≧縲・</p>
                <ul className="list-disc pl-6 space-y-1">
                  <li>莨壼藤諠・ｱ縺ｮ邂｡逅・・險倬鹸</li>
                  <li>莨壼藤蜈･莨壽凾縺ｮ蜀・ｮｹ遒ｺ隱・蟇ｾ雎｡險ｭ蛯吶・蟆主・譎よ悄縲∽ｻ悶・繝励Ο繧ｰ繝ｩ繝蝙九・繝ｭ繧ｸ繧ｧ繧ｯ繝医→縺ｮ莨壼藤縺ｮ驥崎､・匳骭ｲ遲・</li>
                  <li>謗貞・蜑頑ｸ帶ｴｻ蜍輔Μ繧ｹ繝医・菴懈・</li>
                  <li>繝｢繝九ち繝ｪ繝ｳ繧ｰ縺ｮ螳滓命 (繝｢繝九ち繝ｪ繝ｳ繧ｰ繝・・繧ｿ縺ｮ蜿朱寔)</li>
                  <li>繝｢繝九ち繝ｪ繝ｳ繧ｰ蝣ｱ蜻雁､ (謗貞・蜑頑ｸ幃㍼遲・ 縺ｮ邂怜ｮ・</li>
                  <li>繝｢繝九ち繝ｪ繝ｳ繧ｰ蝣ｱ蜻頑嶌縺ｮ菴懈・縺ｨ蟇ｾ蠢・(蟇ｩ譟ｻ蟇ｾ蠢懃ｭ・</li>
                  <li>・ｪ・阪け繝ｬ繧ｸ繝・ヨ蛻ｶ蠎ｦ莠句漁螻縺ｸ縺ｮ蜷・ｨｮ逕ｳ隲・</li>
                  <li>繧ｯ繝ｬ繧ｸ繝・ヨ螢ｲ雋ｷ</li>
                  <li>繧ｯ繝ｬ繧ｸ繝・ヨ螢ｲ蜊ｴ逶翫・豢ｻ逕ｨ</li>
                  <li>莨壼藤縺ｮ騾莨壽焔邯夲ｼ郁｣懷勧驥大女邨ｦ閠・ｒ蟇ｾ雎｡縺ｨ縺励◆繝励Ο繧ｸ繧ｧ繧ｯ繝育ｭ峨〒騾莨壹ｒ遖√★繧句ｴ蜷医↑縺ｩ驕ｩ螳懊＃菫ｮ豁｣縺上□縺輔＞・・</li>
                </ul>
              </section>

              <section>
                <h4 className="font-semibold mb-2">隨ｬ3譚｡縺ｮ3</h4>
                <p>驕句霧繝ｻ邂｡逅・・→縺励※蠢・ｦ√↑莠句漁縺ｯ繝励Ο繧ｰ繝ｩ繝蝙矩°蝟ｶ繝ｻ邂｡逅・・錐縺ｫ縺翫＞縺ｦ陦後≧縲・</p>
              </section>

              <section>
                <h3 className="font-semibold mb-2">(莨壼藤)</h3>
                <h4 className="font-semibold mb-2">隨ｬ4譚｡</h4>
                <p>譛ｬ隕冗ｴ・↓縺翫＞縺ｦ縲∽ｼ壼藤縺ｨ縺ｯ縲∵悽隕冗ｴ・↓蜷梧э縺ｮ荳翫・°蝟ｶ繝ｻ邂｡逅・・↓蜈･莨壹ｒ逕ｳ縺苓ｾｼ繧薙□閠・ｒ縺・≧縲・</p>
              </section>

              <section>
                <h4 className="font-semibold mb-2">隨ｬ4譚｡縺ｮ2</h4>
                <p className="mb-2">莨壼藤縺ｯ谺｡縺ｫ謗ｲ縺偵ｋ蜈ｨ縺ｦ縺ｮ隕∽ｻｶ繧呈ｺ縺溘☆繧ゅ・縺ｨ縺吶ｋ縲・</p>
                <ul className="list-decimal pl-6 space-y-1">
                  <li>莠区･ｭ謇遲峨〒縺ゅｋ縺薙→・亥ｮｶ蠎ｭ縺ｧ縺ｪ縺・％縺ｨ・峨・</li>
                  <li>螟ｪ髯ｽ蜈臥匱髮ｻ險ｭ蛯吶ｒ險ｭ鄂ｮ縺励∫匱髮ｻ縺輔ｌ縺滄崕蜉帙・蜈ｨ驛ｨ蜿医・荳驛ｨ繧定・螳ｶ豸郁ｲｻ縺励※縺・ｋ縺薙→縲・</li>
                  <li>螟ｪ髯ｽ蜈臥匱髮ｻ險ｭ蛯吶・遞ｼ蜒埼幕蟋区律・井ｸ肴・縺ｪ蝣ｴ蜷医・險ｭ鄂ｮ譌･・峨′縲∽ｼ壼藤隕冗ｴ・粋諢乗律 (蜈･莨壼ｱ頑署蜃ｺ譌･) 縺ｮ2蟷ｴ蜑阪・譌･莉･髯阪〒縺ゅｋ縺薙→縲・</li>
                  <li>・ｪ・阪け繝ｬ繧ｸ繝・ヨ蛻ｶ蠎ｦ縺ｫ縺翫￠繧句推遞ｮ逕ｳ隲九↓髫帙＠縲∵悽蜈･莨壼ｱ翫↓險倩ｼ峨＆繧後◆諠・ｱ繧偵・°蝟ｶ繝ｻ邂｡逅・・′菴ｿ逕ｨ縺吶ｋ縺薙→縺ｫ蜷梧э縺吶ｋ縺薙→縲・</li>
                  <li>・ｪ・阪け繝ｬ繧ｸ繝・ヨ蛻ｶ蠎ｦ縺ｫ縺翫￠繧句推遞ｮ逕ｳ隲九↓髫帙＠縲∵悽蜈･莨壼ｱ翫↓險倩ｼ峨＆繧後◆莉･螟悶・諠・ｱ縺ｫ縺､縺・※縲・°蝟ｶ繝ｻ邂｡逅・・′蠢・ｦ√→縺吶ｋ蝣ｴ蜷医・謠蝉ｾ帙☆繧九％縺ｨ縺ｫ蜷梧э縺吶ｋ縺薙→縲・</li>
                  <li>螟ｪ髯ｽ蜈臥匱髮ｻ險ｭ蛯吶ｒ菴ｿ逕ｨ縺吶ｋ縺薙→縺ｫ繧医ｋ閾ｪ螳ｶ豸郁ｲｻ蛻・↓縺､縺・※縺ｮ迺ｰ蠅・ｾ｡蛟､ (貂ｩ螳､蜉ｹ譫懊ぎ繧ｹ謗貞・驥上・蜑頑ｸ帛柑譫・・ｪ・阪け繝ｬ繧ｸ繝・ヨ) 繧帝°蝟ｶ繝ｻ邂｡逅・・∈隴ｲ貂｡縺吶ｋ縺薙→縲√◎縺ｮ邨先棡縺ｨ縺励※縲悟､ｪ髯ｽ蜈臥匱髮ｻ險ｭ蛯吶ｒ菴ｿ逕ｨ縺吶ｋ縺薙→縺ｧ貂ｩ螳､蜉ｹ譫懊ぎ繧ｹ謗貞・驥上ｒ蜑頑ｸ帙阪＠縺溘％縺ｨ繧剃ｼ壼藤縺御ｸｻ蠑ｵ縺ｧ縺阪↑縺上↑繧九％縺ｨ縺ｫ蜷梧э縺吶ｋ縺薙→縲・</li>
                  <li>譛ｬ莨壹↓逋ｻ骭ｲ縺吶ｋ螟ｪ髯ｽ蜈臥匱髮ｻ險ｭ蛯吶′縲∽ｻ悶・鬘樔ｼｼ蛻ｶ蠎ｦ蜿翫・・ｪ・阪け繝ｬ繧ｸ繝・ヨ蛻ｶ蠎ｦ縺ｫ縺翫￠繧倶ｻ悶・繝励Ο繧ｸ繧ｧ繧ｯ繝医・縺・★繧後↓縺翫＞縺ｦ繧ら匳骭ｲ縺輔ｌ縺ｦ縺・↑縺・％縺ｨ縲・</li>
                  <li>螟ｪ髯ｽ蜈臥匱髮ｻ險ｭ蛯吶ｒ蛻ｩ逕ｨ縺吶ｋ莠区･ｭ謇遲峨↓縺翫＞縺ｦ縲∝ｸｸ逕ｨ縺ｮ閾ｪ螳ｶ逋ｺ髮ｻ險ｭ蛯吶ｒ蛻ｩ逕ｨ縺励※縺・↑縺・％縺ｨ縲・</li>
                </ul>
              </section>

              <section>
                <h3 className="font-semibold mb-2">(・ｪ・阪け繝ｬ繧ｸ繝・ヨ螢ｲ蜊ｴ逶翫・蜿悶ｊ謇ｱ縺・</h3>
                <h4 className="font-semibold mb-2">隨ｬ5譚｡</h4>
                <p>・ｪ・阪け繝ｬ繧ｸ繝・ヨ縺ｮ螢ｲ雋ｷ縺ｫ繧医ｋ螢ｲ蜊ｴ逶翫・縲∝茜逕ｨ逶ｮ逧・↓豢ｻ逕ｨ縺吶ｋ縺薙→縺ｨ縺吶ｋ縲・</p>
                <p className="text-xs text-muted-foreground mt-1">窶ｻ蛻ｩ逕ｨ逶ｮ逧・・縲√・繝ｭ繧ｸ繧ｧ繧ｯ繝医↓縺翫￠繧区ｴｻ逕ｨ莠亥ｮ壹・螳滄圀縺ｮ豢ｻ逕ｨ譁ｹ豕輔↓蠢懊§縺ｦ縺比ｿｮ豁｣縺上□縺輔＞縲・</p>
              </section>

              <section>
                <h3 className="font-semibold mb-2">(驕句霧繝ｻ邂｡逅・・∈縺ｮ蜊泌鴨)</h3>
                <h4 className="font-semibold mb-2">隨ｬ6譚｡</h4>
                <p className="mb-2">莨壼藤縺ｯ縲・°蝟ｶ繝ｻ邂｡逅・・′豎ゅａ繧九→縺阪・縲∵ｬ｡縺ｫ謗ｲ縺偵ｋ蜈ｨ縺ｦ縺ｮ莠矩・↓縺､縺・※蜷梧э縺励∝鵠蜉帙＠縺ｪ縺代ｌ縺ｰ縺ｪ繧峨↑縺・・</p>
                <ul className="list-decimal pl-6 space-y-1">
                  <li>・ｪ・阪け繝ｬ繧ｸ繝・ヨ蛻ｶ蠎ｦ縺ｫ縺翫￠繧句推遞ｮ逕ｳ隲九↓髫帙＠縲・°蝟ｶ繝ｻ邂｡逅・・′蠢・ｦ√→縺吶ｋ諠・ｱ繧呈署萓帙☆繧九％縺ｨ縲・</li>
                  <li>・ｪ・阪け繝ｬ繧ｸ繝・ヨ縺ｮ隱崎ｨｼ縺ｫ髫帙＠縲∝ｯｩ譟ｻ讖滄未縺悟ｿ・ｦ√↓蠢懊§縺ｦ螳滓命縺吶ｋ迴ｾ蝨ｰ隱ｿ譟ｻ (螟ｪ髯ｽ蜈臥匱髮ｻ險ｭ蛯吶↓髢｢縺吶ｋ迴ｾ蝨ｰ遒ｺ隱咲ｭ峨ｒ縺・≧) 繧貞女縺代ｋ縺薙→縲・</li>
                  <li>縺昴・莉悶∵悽莨壹・驕句霧蜿翫・邂｡逅・↓髢｢縺励※蠢・ｦ√↑縺薙→縲・</li>
                </ul>
              </section>

              <section>
                <h3 className="font-semibold mb-2">(蝣ｱ蜻・</h3>
                <h4 className="font-semibold mb-2">隨ｬ7譚｡</h4>
                <p className="mb-2">驕句霧繝ｻ邂｡逅・・・縲∵ｬ｡縺ｫ謗ｲ縺偵ｋ莠矩・↓縺､縺・※縲∽ｼ壼藤縺ｫ蟇ｾ縺励※蟷ｴ1蝗槫ｱ蜻翫☆繧九％縺ｨ縺ｨ縺吶ｋ縲・</p>
                <ul className="list-decimal pl-6 space-y-1">
                  <li>・ｪ・阪け繝ｬ繧ｸ繝・ヨ蛻ｶ蠎ｦ隱崎ｨｼ蟋泌藤莨壹∈縺ｮ螳溽ｸｾ蝣ｱ蜻雁所縺ｳ隱崎ｨｼ逕ｳ隲九・邨先棡縺ｫ縺､縺・※</li>
                  <li>・ｪ・阪け繝ｬ繧ｸ繝・ヨ縺ｮ豢ｻ逕ｨ逕ｨ騾斐↓縺､縺・※</li>
                </ul>
              </section>

              <section>
                <h4 className="font-semibold mb-2">隨ｬ7譚｡縺ｮ2</h4>
                <p>蜑埼・・蝣ｱ蜻翫・縲・°蝟ｶ繝ｻ邂｡逅・・′繧ｦ繧ｧ繝悶し繧､繝井ｸ翫↓邨先棡讎りｦ√ｒ謗ｲ霈峨☆繧九％縺ｨ繧剃ｻ･縺ｦ陦後≧縺薙→縺ｨ縺吶ｋ縲・</p>
              </section>

              <section>
                <h3 className="font-semibold mb-2">(險ｭ蛯吶・蜃ｦ蛻・ｭ・</h3>
                <h4 className="font-semibold mb-2">隨ｬ8譚｡</h4>
                <p className="mb-2">莨壼藤縺ｯ縲∫ｬｬ11譚｡縺ｫ隕丞ｮ壹☆繧倶ｼ壼藤雉・ｼ縺ｮ譛牙柑譛滄俣蜀・↓縺翫＞縺ｦ縲∵ｬ｡縺ｮ蜷・捷縺ｮ荳縺ｫ隧ｲ蠖薙☆繧九→縺阪・縲√◎縺ｮ譌ｨ繧帝°蝟ｶ繝ｻ邂｡逅・・↓螻翫￠蜃ｺ縺ｪ縺代ｌ縺ｰ縺ｪ繧峨↑縺・・</p>
                <ul className="list-decimal pl-6 space-y-1">
                  <li>螟ｪ髯ｽ蜈臥匱髮ｻ險ｭ蛯吶′豈謳阪＆繧後∝処縺ｯ貊・､ｱ縺励◆縺ｨ縺阪・</li>
                  <li>螟ｪ髯ｽ蜈臥匱髮ｻ險ｭ蛯吶ｒ蜃ｦ蛻・(螢ｲ蜊ｴ縲∬ｭｲ貂｡縲∽ｺ､謠帙∬ｲｸ莉伜処縺ｯ諡・ｿ昴↓萓帙☆繧九％縺ｨ) 縺励ｈ縺・→縺吶ｋ縺ｨ縺阪・</li>
                </ul>
              </section>

              <section>
                <h3 className="font-semibold mb-2">(騾莨・</h3>
                <p className="text-xs text-muted-foreground mb-2">窶ｻ陬懷勧驥大女邨ｦ閠・ｒ蟇ｾ雎｡縺ｨ縺励◆繝励Ο繧ｸ繧ｧ繧ｯ繝育ｭ峨〒騾莨壹ｒ遖√★繧句ｴ蜷医ｄ縲√◎縺ｮ莉悶・繝ｭ繧ｸ繧ｧ繧ｯ繝医・螳滄圀縺ｫ蠢懊§縺ｦ驕ｩ螳懊＃菫ｮ豁｣縺上□縺輔＞縲・</p>
                <h4 className="font-semibold mb-2">隨ｬ9譚｡</h4>
                <p>莨壼藤縺ｯ縲∵悽莨壹ｒ騾莨壹＠繧医≧縺ｨ縺吶ｋ縺ｨ縺阪・縲・°蝟ｶ繝ｻ邂｡逅・・↓縺昴・譌ｨ繧貞ｱ翫￠蜃ｺ縲√◎縺ｮ謇ｿ隱阪ｒ蠕励↑縺代ｌ縺ｰ縺ｪ繧峨↑縺・・</p>
              </section>

              <section>
                <h4 className="font-semibold mb-2">隨ｬ9譚｡縺ｮ2</h4>
                <p className="mb-2">驕句霧繝ｻ邂｡逅・・・縲∽ｼ壼藤縺梧ｬ｡縺ｮ蜷・捷縺ｮ荳縺ｫ隧ｲ蠖薙☆繧九→縺阪・縲∝ｽ楢ｩｲ莨壼藤繧帝莨壹＆縺帙ｋ縺薙→縺後〒縺阪ｋ縲・</p>
                <ul className="list-decimal pl-6 space-y-1">
                  <li>隨ｬ4譚｡隨ｬ2鬆・↓謗ｲ縺偵ｋ隕∽ｻｶ繧呈ｺ縺溘＠縺ｦ縺・↑縺・→縺阪・</li>
                  <li>蜑肴擅縺ｮ螻雁・縺後≠縺｣縺溘→縺阪・</li>
                  <li>莨壼藤縺ｮ陦檎ぜ縺梧悽莨壹・逶ｮ逧・↓闡励＠縺冗嶌蠢懊＠縺上↑縺・→隱阪ａ繧九→縺阪・</li>
                </ul>
              </section>

              <section>
                <h3 className="font-semibold mb-2">(莨夊ｲｻ)</h3>
                <h4 className="font-semibold mb-2">隨ｬ10譚｡</h4>
                <p>譛ｬ莨壹・莨夊ｲｻ縺ｯ辟｡譁吶→縺吶ｋ縲・</p>
              </section>

              <section>
                <h3 className="font-semibold mb-2">(莨壼藤雉・ｼ縺ｮ譛牙柑譛滄俣)</h3>
                <h4 className="font-semibold mb-2">隨ｬ11譚｡</h4>
                <p>莨壼藤雉・ｼ縺ｮ譛牙柑譛滄俣縺ｯ蜈･莨夂筏霎ｼ譌･闍･縺励￥縺ｯ繝励Ο繧ｸ繧ｧ繧ｯ繝医′螳滓命縺輔ｌ縺滓律・遺ｻ・峨・縺・★繧後°驕・＞譌･縺九ｉ8蟷ｴ髢薙→縺吶ｋ縲ゅ◆縺縺励・°蝟ｶ繝ｻ邂｡逅・・′謠仙・縺励・ｼｪ・阪け繝ｬ繧ｸ繝・ヨ蛻ｶ蠎ｦ邂｡逅・・′謇ｿ隱阪＠縺溘∬ｪ崎ｨｼ蟇ｾ雎｡譛滄俣縺ｮ蟒ｶ髟ｷ縺ｫ菫ゅｋ繝励Ο繧ｸ繧ｧ繧ｯ繝郁ｨ育判螟画峩螻翫↓險倩ｼ峨＆繧後◆謇狗ｶ壹″縺ｫ豐ｿ縺｣縺ｦ蟒ｶ髟ｷ繧定｡後≧蝣ｴ蜷医∽ｼ壼藤雉・ｼ縺ｯ8蟷ｴ髢灘ｻｶ髟ｷ縺輔ｌ繧九ｂ縺ｮ縺ｨ縺吶ｋ縲・</p>
                <p className="text-xs text-muted-foreground mt-1">窶ｻ繝励Ο繧ｸ繧ｧ繧ｯ繝医′螳滓命縺輔ｌ縺滓律縺ｯ縲∵ｸｩ螳､蜉ｹ譫懊ぎ繧ｹ謗貞・驥上・蜑頑ｸ帛処縺ｯ貂ｩ螳､蜉ｹ譫懊ぎ繧ｹ縺ｮ髯､蜴ｻ繧偵ｂ縺溘ｉ縺呎ｴｻ蜍輔′螳溯ｳｪ逧・↓髢句ｧ九＆繧後◆譌･縺ｨ縺吶ｋ縲・</p>
              </section>

              <section>
                <h3 className="font-semibold mb-2">(蛟倶ｺｺ諠・ｱ繝ｻ驥崎ｦ∵ュ蝣ｱ遲峨・蜿匁桶縺・</h3>
                <h4 className="font-semibold mb-2">隨ｬ12譚｡</h4>
                <p>驕句霧繝ｻ邂｡逅・・・縲√≠繧峨°縺倥ａ莨壼藤縺ｮ蜷梧э縺後≠繧句ｴ蜷医∵悽隕冗ｴ・↓隕丞ｮ壹☆繧句ｴ蜷亥処縺ｯ豕穂ｻ､遲峨↓蝓ｺ縺･縺剰ｦ∬ｫ九′縺ゅｋ蝣ｴ蜷医ｒ髯､縺阪∽ｼ壼藤縺九ｉ蠕励ｉ繧後◆蛟倶ｺｺ諠・ｱ縲・㍾隕∵ュ蝣ｱ蜿翫・縺薙ｌ繧峨↓鬘槭☆繧区ュ蝣ｱ繧堤ｬｬ荳芽・↓謠蝉ｾ帛処縺ｯ髢狗､ｺ縺励↑縺・％縺ｨ縺ｨ縺吶ｋ縲・</p>
              </section>

              <section>
                <h3 className="font-semibold mb-2">(蟋比ｻｻ)</h3>
                <h4 className="font-semibold mb-2">隨ｬ13譚｡</h4>
                <p>譛ｬ隕冗ｴ・↓螳壹ａ繧九ｂ縺ｮ縺ｮ縺ｻ縺九∵悽莨壹・蜈･莨壹↓髢｢縺怜ｿ・ｦ√↑莠矩・・驕句霧繝ｻ邂｡逅・・′螳壹ａ繧九・</p>
              </section>

              <section className="mt-8">
                <h3 className="font-semibold mb-2">髯・蜑・</h3>
                <p>譛ｬ隕冗ｴ・・縲〆YYY蟷ｴMM譛・D (繝励Ο繧ｸ繧ｧ繧ｯ繝育匳骭ｲ逕ｳ隲区律) 縺九ｉ譁ｽ陦後☆繧九・</p>
              </section>
            </div>
          </div>

          <div className="flex items-start space-x-3 mt-6">
            <Card className="max-w-sm mx-auto mt-6 p-6 rounded-lg">
              
                <Form {...form}>
                  <FormField
                    control={form.control}
                    name="finalTermsAgreed"
                    render={({ field }) => {
                      const { value, ...rest } = field;
                      return (
                        <FormItem className="flex items-center space-x-2">
                          <FormControl>
                            <input
                              type="checkbox"
                              id="finalTermsAgreed"
                              {...rest}
                              checked={value}
                              onChange={(e) => field.onChange(e.target.checked)}
                              className="h-5 w-5 rounded-sm border-input align-middle text-primary focus:ring-2 focus:ring-ring"
                            />
                          </FormControl>
                          <div className="flex flex-col">
                            <FormLabel htmlFor="finalTermsAgreed" className="text-sm font-medium leading-none">
                              蜷梧э縺励∪縺・                            </FormLabel>
                            <FormMessage className="mt-1 text-xs text-destructive" /> {/* displays validation error */}
                          </div>
                        </FormItem>
                      );
                    }}
                  />
                </Form>
              
            </Card>
          </div>
        </CardContent>
      </Card>
    </div>
  );
});

FinalTermsStep.displayName = "FinalTermsStep";



