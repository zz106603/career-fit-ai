import { z } from 'zod'

const passwordSchema = z
  .string()
  .min(8, '비밀번호는 8자 이상 입력해 주세요.')
  .max(72, '비밀번호는 72자 이하로 입력해 주세요.')

export const loginSchema = z.object({
  email: z.email('올바른 이메일 주소를 입력해 주세요.'),
  password: z.string().min(1, '비밀번호를 입력해 주세요.'),
})

export const signupSchema = z
  .object({
    email: z.email('올바른 이메일 주소를 입력해 주세요.'),
    password: passwordSchema,
    passwordConfirm: z.string().min(1, '비밀번호 확인을 입력해 주세요.'),
  })
  .refine((values) => values.password === values.passwordConfirm, {
    path: ['passwordConfirm'],
    message: '비밀번호가 일치하지 않습니다.',
  })

export type LoginFormValues = z.infer<typeof loginSchema>
export type SignupFormValues = z.infer<typeof signupSchema>
