import { forwardRef, useEffect, useImperativeHandle, useRef } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { z } from "zod";
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from "react-hook-form";
import { Form, FormItem, FormField, FormLabel, FormControl, FormMessage } from "@/components/ui/form";

const contractStepSchema = z.object({
  businessName: z.string().nonempty("莠区･ｭ閠・錐繧貞・蜉帙＠縺ｦ縺上□縺輔＞"),
  representativeName: z.string().nonempty("莉｣陦ｨ閠・錐繧貞・蜉帙＠縺ｦ縺上□縺輔＞"),
  representativePosition: z.string().nonempty("莉｣陦ｨ閠・ｽｹ閨ｷ繧貞・蜉帙＠縺ｦ縺上□縺輔＞"),
  contractAgreed: z.boolean().refine(val => val === true, "蜷梧э縺励※縺上□縺輔＞"),
})

type ContractStepData = z.infer<typeof contractStepSchema>;

export interface ContractStepRef {
  triggerValidation: () => Promise<boolean>;
}

export type ContractStepFormValues = {
  contractAgreed: boolean;
  businessName: string;
  representativePosition: string;
  representativeName: string;
}

interface ContractStepProps {
  formValues: Partial<ContractStepFormValues>;
  onValueChange: (values: Partial<ContractStepFormValues>) => void;
  errors?: Record<string, string>;
}


export const ContractStep = forwardRef<ContractStepRef, ContractStepProps>(({
formValues,
  onValueChange,
}, ref) => {

  const form = useForm<ContractStepData>({
    // resolver: zodResolver(contractStepSchema),
    mode: "onChange",
    defaultValues: {
      contractAgreed: false,
      businessName: "",
      representativePosition: "",
      representativeName: "",
    },
    ...(formValues as Partial<ContractStepData>),
  });
  const watchedValues = form.watch();

  useImperativeHandle(ref, () => ({
    triggerValidation: async () => {
      return await form.trigger(); // parent can call this to validate
    },
  }), [form]);

  // Reset form with saved values when component mounts or formValues changes
  useEffect(() => {
    form.reset(formValues as ContractStepFormValues);
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
        <h2 className="text-2xl font-bold mb-2">環境社会配慮を行い持続可能性を確保することの同意書 <span className="ml-1 text-destructive">*</span></h2>
        <p className="text-muted-foreground">以下の内容をご確認の上、同意してください</p>
      </div>

      <Card>
        <CardContent className="pt-6">
          
          <div className="p-6 border rounded-lg bg-muted/30 space-y-4">
            <div className="text-sm leading-relaxed space-y-1">
              
              <p>譬ｪ蠑丈ｼ夂､ｾ繝翫Ξ繝・ず繧ｯ繝ｪ繧ｨ繝ｼ繧ｷ繝ｧ繝ｳ繝・け繝弱Ο繧ｸ縲蠕｡荳ｭ</p>
              <br />

              <div className="grid grid-cols-1 md:grid-cols-[auto_1fr] gap-2 items-center">
                <Label htmlFor="businessName" className="text-sm font-medium whitespace-nowrap">
                  莠区･ｭ閠・錐: 
                </Label>
                <Form {...form}>
                  <FormField
                    control={form.control}
                    name="businessName"
                    render={({ field }) => (
                      <FormItem>
                        <FormControl>
                          <Input
                            id="businessName"
                            {...field}  // connects RHF to the input
                            placeholder="事業者名を入力"
                            className="w-48 h-9"
                          />
                        </FormControl>
                        <FormMessage /> {/* displays validation error */}
                      </FormItem>
                    )}
                  />
                </Form>

                <Label htmlFor="representativeName" className="text-sm font-medium whitespace-nowrap">
                  莉｣陦ｨ閠・錐: 
                </Label>
                <Form {...form}>
                  <FormField
                    control={form.control}
                    name="representativeName"
                    render={({ field }) => (
                      <FormItem>
                        <FormControl>
                          <Input
                            id="representativeName"
                            {...field} 
                            placeholder="代表者名を入力"
                            className="w-48 h-9"
                          />
                        </FormControl>
                        <FormMessage /> {/* displays validation error */}
                      </FormItem>
                    )}
                  />
                </Form>

                <Label htmlFor="representativePosition" className="text-sm font-medium whitespace-nowrap">
                  莉｣陦ｨ閠・ｽｹ閨ｷ: 
                </Label>
                <Form {...form}>
                  <FormField
                    control={form.control}
                    name="representativePosition"
                    render={({ field }) => (
                      <FormItem>
                        <FormControl>
                          <Input
                            id="representativePosition"
                            {...field}  
                            placeholder="役職を入力"
                            className="w-48 h-9"
                          />
                        </FormControl>
                        <FormMessage /> {/* displays validation error */}
                      </FormItem>
                    )}
                  />
                </Form>
              
              </div>

              <br />       
              <p>
                蝗ｽ蜀・↓縺翫￠繧句慍逅・ｸｩ證門喧蟇ｾ遲悶・縺溘ａ縺ｮ謗貞・蜑頑ｸ帙・蜷ｸ蜿朱㍼隱崎ｨｼ蛻ｶ蠎ｦ・茨ｼｪ・阪け繝ｬ繧ｸ繝・ヨ蛻ｶ蠎ｦ・峨↓縲∵婿豕戊ｫ胞N-R-002・亥､ｪ髯ｽ蜈臥匱髮ｻ險ｭ蛯吶・蟆主・・峨↓蝓ｺ縺･縺上・繝ｭ繧ｸ繧ｧ繧ｯ繝医ｒ逋ｻ骭ｲ縺励※螳滓命縺吶ｋ縺ｫ縺ゅ◆繧翫∽ｺｺ髢薙・蛛･蠎ｷ縺ｨ螳牙・縲∬・辟ｶ迺ｰ蠅・∫､ｾ莨壹∈縺ｮ蠖ｱ髻ｿ繧貞屓驕ｿ縺ｾ縺溘・譛蟆丞喧縺励∝女縺大・繧後ｋ縺薙→縺後〒縺阪↑縺・ｈ縺・↑蠖ｱ髻ｿ繧偵ｂ縺溘ｉ縺吶％縺ｨ縺後↑縺・ｈ縺・∫腸蠅・､ｾ莨夐・諷ｮ繧定｡後＞謖∫ｶ壼庄閭ｽ諤ｧ繧堤｢ｺ菫昴☆繧九◆繧√・              </p>
              <br />           
              <div className="pl-4 space-y-1">
                <p>繝ｻ繧ｨ繝阪Ν繧ｮ繝ｼ縺ｮ菴ｿ逕ｨ縺ｮ蜷育炊蛹門所縺ｳ髱槫喧遏ｳ繧ｨ繝阪Ν繧ｮ繝ｼ縺ｸ縺ｮ霆｢謠帷ｭ峨↓髢｢縺吶ｋ豕募ｾ・</p>
                <p>繝ｻ迚ｹ螳壼ｷ･蝣ｴ縺ｫ縺翫￠繧句・螳ｳ髦ｲ豁｢邨・ｹ斐・謨ｴ蛯吶↓髢｢縺吶ｋ豕募ｾ・</p>
                <p>繝ｻ迺ｰ蠅・渕譛ｬ豕・</p>
                <p>繝ｻ蟒ｺ遽牙渕貅匁ｳ・</p>
                <p>繝ｻ髮ｻ豌嶺ｺ区･ｭ豕・</p>
                <p>繝ｻ譎ｯ隕ｳ豕・</p>
                <p>繝ｻ豸磯亟豕・</p>
                <p>繝ｻ蜉ｴ蜒榊ｮ牙・陦帷函豕・</p>
                <p>繝ｻ蝨ｰ逅・ｸｩ證門喧蟇ｾ遲悶・謗ｨ騾ｲ縺ｫ髢｢縺吶ｋ豕募ｾ・</p>
              </div>
              <br />     
              <p>
                縺昴・莉夜未騾｣豕穂ｻ､遲峨ｒ驕ｵ螳医☆繧九％縺ｨ繧定ｪ鍋ｴ・＠縺ｾ縺吶・              </p>

              <p className="text-right pt-4">莉･荳・</p>
            </div>
          </div>

          <div className="flex justify-center mt-6">

            <Card className="max-w-sm mx-auto mt-6 p-6 rounded-lg">
              
                <Form {...form}>
                  <FormField
                    control={form.control}
                    name="contractAgreed"
                    render={({ field }) => {
                      const { value, ...rest } = field;
                      return (
                        <FormItem className="flex items-center space-x-2">
                          <FormControl>
                            <input
                              type="checkbox"
                              id="contractAgreed"
                              {...rest}
                              checked={value}
                              onChange={(e) => field.onChange(e.target.checked)}
                              className="w-5 h-5 rounded-sm border-gray-300 text-blue-600 focus:ring-2 focus:ring-blue-400 align-middle"
                            />
                          </FormControl>
                          <div className="flex flex-col">
                            <FormLabel htmlFor="contractAgreed" className="text-sm font-medium leading-none">
                              蜷梧э縺励∪縺・                            </FormLabel>
                            <FormMessage className="text-xs text-red-500 mt-1" /> {/* displays validation error */}
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

ContractStep.displayName = "ContractStep";

