import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { useRedirectIfLoggedIn } from '@/hooks/useRedirectIfLoggedIn';
import { useState } from 'react';

// Zod schema for login form validation
const loginSchema = z.object({
    companyId: z
        .string()
        .min(1, '会員IDを入力してください')
        .max(50, '会員IDは50文字以内で入力してください'),
    email: z
        .string()
        .min(1, 'メールアドレスを入力してください')
        .max(255, 'メールアドレスは255文字以内で入力してください'),
    password: z
        .string()
        .min(1, 'パスワードを入力してください')
        .min(15, 'パスワードは15文字以上で入力してください')
        .max(100, 'パスワードは100文字以内で入力してください'),
});

export type LoginFormData = z.infer<typeof loginSchema>;


export type LoginFormProps = {
    title?: string;
    onSubmit: (data: LoginFormData) => Promise<void>;
    submitLabel?: string;
};

export function LoginForm({
    title = "ログイン",
    onSubmit,
    submitLabel = "Login",
}: Readonly<LoginFormProps>) {
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const form = useForm<LoginFormData>({
        resolver: zodResolver(loginSchema),
        defaultValues: {
            companyId: '',
            email: '',
            password: '',
        },
    });


    useRedirectIfLoggedIn();

    async function handleSubmit(values: LoginFormData) {
        setError(null);
        setSubmitting(true);
        try {
            await onSubmit(values);
        } catch (err: any) {
            setError(err.message || "Login failed");
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <div className="min-h-screen">
            <div className="bg-primary text-primary-foreground py-6 px-4">
                <div className="max-w-md mx-auto">
                    <h1 className="text-3xl font-bold text-center">{title}</h1>
                </div>
            </div>
            <div className="flex items-center justify-center px-4 py-12">
                <div className="w-full max-w-md animate-slide-up">
                    {error && <p style={{ color: "red" }}>{error}</p>}

                    <Form {...form}>
                        <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4">
                            <FormField
                                control={form.control}
                                name="companyId"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>会員ID</FormLabel>
                                        <FormControl>
                                            <Input
                                                type="text"
                                                placeholder="会員IDを入力"
                                                {...field}
                                            />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="email"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>メールアドレス</FormLabel>
                                        <FormControl>
                                            <Input
                                                type="email"
                                                placeholder="you@company.com"
                                                {...field}
                                            />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="password"
                                render={({ field }) => (
                                    <FormItem>
                                        <div className="flex items-center justify-between">
                                            <FormLabel>パスワード</FormLabel>
                                        </div>
                                        <FormControl>
                                            <Input
                                                type="password"
                                                placeholder="パスワードを入力"
                                                {...field}
                                            />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <Button type="submit" className="w-full" disabled={form.formState.isSubmitting}>
                                {form.formState.isSubmitting ? 'ログイン中...' : 'ログイン'}
                            </Button>
                        </form>
                    </Form>
                    {/* <div className="text-center py-6">
                            <button
                                type="button"
                                onClick={() => navigate('/forgot-password')}
                                className="text-sm text-primary hover:underline"
                            >
                                会員IDまたはパスワードを忘れの方
                            </button>
                        </div>
                        <div className="mt-8 border-t border-gray-300 pt-4 text-center">
                            <a href="/contact" className="text-sm text-primary hover:underline">
                                ログインに関するお問い合わせはこちら
                            </a>
                        </div> */}
                </div>
            </div>
        </div>
    );
};

export default LoginForm;
