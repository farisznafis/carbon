import { forwardRef, useEffect, useImperativeHandle, useRef } from "react";
import { RequiredLabel } from "@/components/forms/shared/RequiredLabel";
import { Card, CardContent } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { z } from "zod";
import { useForm } from "react-hook-form";
import { Form, FormItem, FormField, FormControl, FormMessage } from "@/components/ui/form";
import { FormActions } from "@/components/forms/form-layout";

const FinalTermsSchema = z.object({
  finalTermsAgreed: z.boolean().refine(val => val === true, "同意してください"),
})

type FinalTermsData = z.infer<typeof FinalTermsSchema>;

export interface FinalTermsRef {
  triggerValidation: () => Promise<boolean>;
  getValues: () => FinalTermsValues;
}

export type FinalTermsValues = {
  finalTermsAgreed: boolean;
}

interface FinalTermsStepProps {
  formValues: Partial<FinalTermsValues>;
  isSubmitting: boolean;
  onBack: () => void;
  onNext: () => void;
  onValueChange: (values: Partial<FinalTermsData>) => void;
  errors?: Record<string, string>;
}

export const FinalTermsStep = forwardRef<FinalTermsRef, FinalTermsStepProps>(({
  formValues,
  isSubmitting,
  onBack,
  onNext,
  onValueChange,
  errors = {},
}, ref) => {

  const form = useForm<FinalTermsData>({
      mode: "onChange",
      defaultValues: {
        finalTermsAgreed: false,
        ...formValues,
      },
    });
    const watchedValues = form.watch();
    const serverErrorFieldsRef = useRef<Array<keyof FinalTermsValues>>([]);
  
    useImperativeHandle(ref, () => ({
      triggerValidation: () => form.trigger(),
      getValues: () => form.getValues(),
    }), [form]);
  
    // Reset form with saved values when component mounts or formValues changes
    useEffect(() => {
      form.reset({
        finalTermsAgreed: false,
        ...formValues,
      });
    }, [form, formValues]);

    useEffect(() => {
      serverErrorFieldsRef.current.forEach((fieldName) => {
        form.clearErrors(fieldName);
      });

      const serverErrorFields = Object.entries(errors).flatMap(
        ([fieldName, message]) => {
          if (!(fieldName in form.getValues()) || !message) return [];

          const finalTermsFieldName = fieldName as keyof FinalTermsValues;
          form.setError(finalTermsFieldName, {
            type: 'server',
            message,
          });
          return [finalTermsFieldName];
        },
      );
      serverErrorFieldsRef.current = serverErrorFields;
    }, [errors, form]);
  
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
        <p className="text-foreground">入会規約をご確認の上、同意してください</p>
      </div>

      <Card>
        <CardContent className="pt-6">
          <div className="max-h-[600px] overflow-y-auto p-6 border rounded-lg">
            <div className="text-sm leading-relaxed space-y-6">
              <section>
                <h3 className="font-semibold mb-2">(名称)</h3>
                <h4 className="font-semibold mb-2">第1条</h4>
                <p>本会の名称は、プログラム名 (以下「本会」という) とする。</p>
              </section>

              <section>
                <h3 className="font-semibold mb-2">(目的)</h3>
                <h4 className="font-semibold mb-2">第2条</h4>
                <p>本会は、プログラム型運営・管理者名が実施する「プロジェクト概要」の一環として、会員が太陽光発電設備を使用することで削減された温室効果ガス排出量を、国内における地球温暖化対策のための排出削減・吸収量認証制度 (以下「Ｊ－クレジット制度」という) 実施要綱 (平成25年4月17日経済産業省、環境省、農林水産省策定) に基づきＪ－クレジットとしてＪ－クレジット制度認証委員会 (Ｊ－クレジット制度を管理する経済産業省、環境省、農林水産省が共同で設置した、Ｊ－クレジットの認証を行う委員会) より認証を受けることで、環境価値の「見える化」を図るとともに、創出されたＪ－クレジットをプログラム型運営・管理者名における利用目的に利用することを目的とする。</p>
              </section>

              <section>
                <h3 className="font-semibold mb-2">(運営・管理)</h3>
                <h4 className="font-semibold mb-2">第3条</h4>
                <p>本会の運営・管理はプログラム型運営・管理者名 (以下「運営・管理者」という) が行う。</p>
              </section>

              <section>
                <h4 className="font-semibold mb-2">第3条の2</h4>
                <p className="mb-2">運営・管理者はＪ－クレジット制度に関し、次に掲げる業務を行う。</p>
                <ul className="list-disc pl-6 space-y-1">
                  <li>会員情報の管理・記録</li>
                  <li>会員入会時の内容確認(対象設備の導入時期、他のプログラム型プロジェクトとの会員の重複登録等)</li>
                  <li>排出削減活動リストの作成</li>
                  <li>モニタリングの実施 (モニタリングデータの収集)</li>
                  <li>モニタリング報告値 (排出削減量等) の算定</li>
                  <li>モニタリング報告書の作成と対応 (審査対応等)</li>
                  <li>Ｊ－クレジット制度事務局への各種申請</li>
                  <li>クレジット売買</li>
                  <li>クレジット売却益の活用</li>
                  <li>会員の退会手続（補助金受給者を対象としたプロジェクト等で退会を禁ずる場合など適宜ご修正ください）</li>
                </ul>
              </section>

              <section>
                <h4 className="font-semibold mb-2">第3条の3</h4>
                <p>運営・管理者として必要な事務はプログラム型運営・管理者名において行う。</p>
              </section>

              <section>
                <h3 className="font-semibold mb-2">(会員)</h3>
                <h4 className="font-semibold mb-2">第4条</h4>
                <p>本規約において、会員とは、本規約に同意の上、運営・管理者に入会を申し込んだ者をいう。</p>
              </section>

              <section>
                <h4 className="font-semibold mb-2">第4条の2</h4>
                <p className="mb-2">会員は次に掲げる全ての要件を満たすものとする。</p>
                <ul className="list-decimal pl-6 space-y-1">
                  <li>事業所等であること（家庭でないこと）。</li>
                  <li>太陽光発電設備を設置し、発電された電力の全部又は一部を自家消費していること。</li>
                  <li>太陽光発電設備の稼働開始日（不明な場合は設置日）が、会員規約合意日 (入会届提出日) の2年前の日以降であること。</li>
                  <li>Ｊ－クレジット制度における各種申請に際し、本入会届に記載された情報を、運営・管理者が使用することに同意すること。</li>
                  <li>Ｊ－クレジット制度における各種申請に際し、本入会届に記載された以外の情報について、運営・管理者が必要とする場合は提供することに同意すること。</li>
                  <li>太陽光発電設備を使用することによる自家消費分についての環境価値 (温室効果ガス排出量の削減効果=Ｊ－クレジット) を運営・管理者へ譲渡すること、その結果として「太陽光発電設備を使用することで温室効果ガス排出量を削減」したことを会員が主張できなくなることに同意すること。</li>
                  <li>本会に登録する太陽光発電設備が、他の類似制度及びＪ－クレジット制度における他のプロジェクトのいずれにおいても登録されていないこと。</li>
                  <li>太陽光発電設備を利用する事業所等において、常用の自家発電設備を利用していないこと。</li>
                </ul>
              </section>

              <section>
                <h3 className="font-semibold mb-2">(Ｊ－クレジット売却益の取り扱い)</h3>
                <h4 className="font-semibold mb-2">第5条</h4>
                <p>Ｊ－クレジットの売買による売却益は、利用目的に活用することとする。</p>
                <p className="text-xs text-foreground mt-1">※利用目的は、プロジェクトにおける活用予定・実際の活用方法に応じてご修正ください。</p>
              </section>

              <section>
                <h3 className="font-semibold mb-2">(運営・管理者への協力)</h3>
                <h4 className="font-semibold mb-2">第6条</h4>
                <p className="mb-2">会員は、運営・管理者が求めるときは、次に掲げる全ての事項について同意し、協力しなければならない。</p>
                <ul className="list-decimal pl-6 space-y-1">
                  <li>Ｊ－クレジット制度における各種申請に際し、運営・管理者が必要とする情報を提供すること。</li>
                  <li>Ｊ－クレジットの認証に際し、審査機関が必要に応じて実施する現地調査 (太陽光発電設備に関する現地確認等をいう) を受けること。</li>
                  <li>その他、本会の運営及び管理に関して必要なこと。</li>
                </ul>
              </section>

              <section>
                <h3 className="font-semibold mb-2">(報告)</h3>
                <h4 className="font-semibold mb-2">第7条</h4>
                <p className="mb-2">運営・管理者は、次に掲げる事項について、会員に対して年1回報告することとする。</p>
                <ul className="list-decimal pl-6 space-y-1">
                  <li>Ｊ－クレジット制度認証委員会への実績報告及び認証申請の結果について</li>
                  <li>Ｊ－クレジットの活用用途について</li>
                </ul>
              </section>

              <section>
                <h4 className="font-semibold mb-2">第7条の2</h4>
                <p>前項の報告は、運営・管理者がウェブサイト上に結果概要を掲載することを以て行うこととする。</p>
              </section>

              <section>
                <h3 className="font-semibold mb-2">(設備の処分等)</h3>
                <h4 className="font-semibold mb-2">第8条</h4>
                <p className="mb-2">会員は、第11条に規定する会員資格の有効期間内において、次の各号の一に該当するときは、その旨を運営・管理者に届け出なければならない。</p>
                <ul className="list-decimal pl-6 space-y-1">
                  <li>太陽光発電設備が毀損され、又は滅失したとき。</li>
                  <li>太陽光発電設備を処分 (売却、譲渡、交換、貸付又は担保に供すること) しようとするとき。</li>
                </ul>
              </section>

              <section>
                <h3 className="font-semibold mb-2">(退会)</h3>
                <p className="text-xs text-foreground mb-2">※補助金受給者を対象としたプロジェクト等で退会を禁ずる場合や、その他プロジェクトの実際に応じて適宜ご修正ください。</p>
                <h4 className="font-semibold mb-2">第9条</h4>
                <p>会員は、本会を退会しようとするときは、運営・管理者にその旨を届け出、その承認を得なければならない。</p>
              </section>

              <section>
                <h4 className="font-semibold mb-2">第9条の2</h4>
                <p className="mb-2">運営・管理者は、会員が次の各号の一に該当するときは、当該会員を退会させることができる。</p>
                <ul className="list-decimal pl-6 space-y-1">
                  <li>第4条第2項に掲げる要件を満たしていないとき。</li>
                  <li>前条の届出があったとき。</li>
                  <li>会員の行為が本会の目的に著しく相応しくないと認めるとき。</li>
                </ul>
              </section>

              <section>
                <h3 className="font-semibold mb-2">(会費)</h3>
                <h4 className="font-semibold mb-2">第10条</h4>
                <p>本会の会費は無料とする。</p>
              </section>

              <section>
                <h3 className="font-semibold mb-2">(会員資格の有効期間)</h3>
                <h4 className="font-semibold mb-2">第11条</h4>
                <p>会員資格の有効期間は入会申込日若しくはプロジェクトが実施された日（※）のいずれか遅い日から8年間とする。ただし、運営・管理者が提出し、Ｊ－クレジット制度管理者が承認した、認証対象期間の延長に係るプロジェクト計画変更届に記載された手続きに沿って延長を行う場合、会員資格は8年間延長されるものとする。</p>
                <p className="text-xs text-foreground mt-1">※プロジェクトが実施された日は、温室効果ガス排出量の削減又は温室効果ガスの除去をもたらす活動が実質的に開始された日とする。</p>
              </section>

              <section>
                <h3 className="font-semibold mb-2">(個人情報・重要情報等の取扱い)</h3>
                <h4 className="font-semibold mb-2">第12条</h4>
                <p>運営・管理者は、あらかじめ会員の同意がある場合、本規約に規定する場合又は法令等に基づく要請がある場合を除き、会員から得られた個人情報、重要情報及びこれらに類する情報を第三者に提供又は開示しないこととする。</p>
              </section>

              <section>
                <h3 className="font-semibold mb-2">(委任)</h3>
                <h4 className="font-semibold mb-2">第13条</h4>
                <p>本規約に定めるもののほか、本会の入会に関し必要な事項は運営・管理者が定める。</p>
              </section>

              <section className="mt-8">
                <h3 className="font-semibold mb-2">附　則</h3>
                <p>本規約は、YYYY年MM月DD (プロジェクト登録申請日) から施行する。</p>
              </section>
            </div>
          </div>

          <Form {...form}>
            <FormField
              control={form.control}
              name="finalTermsAgreed"
              render={({ field }) => (
                <FormItem className="mt-6 rounded-lg border border-border p-4">
                  <div className="flex items-center gap-3">
                    <FormControl>
                      <Checkbox
                        id="finalTermsAgreed"
                        checked={field.value}
                        onCheckedChange={(checked) =>
                          field.onChange(checked === true)
                        }
                        aria-required="true"
                      />
                    </FormControl>
                    <RequiredLabel
                      htmlFor="finalTermsAgreed"
                      className="cursor-pointer"
                    >
                      同意します
                    </RequiredLabel>
                  </div>
                  <FormMessage className="mt-2" />
                </FormItem>
              )}
            />
            <FormActions
              pendingActionId={isSubmitting ? "next" : null}
              actions={[
                {
                  id: "back",
                  label: "戻る",
                  variant: "outline",
                  onClick: onBack,
                },
                {
                  id: "next",
                  label: "確認へ",
                  pendingLabel: "確認中...",
                  onClick: onNext,
                },
              ]}
            />
          </Form>
        </CardContent>
      </Card>
    </div>
  );
});

FinalTermsStep.displayName = "FinalTermsStep";



