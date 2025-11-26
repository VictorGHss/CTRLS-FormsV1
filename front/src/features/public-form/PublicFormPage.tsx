import {useState, useEffect} from 'react';
import {useParams} from 'react-router-dom';
import {useQuery, useMutation} from '@tanstack/react-query';
import {publicFormsApi} from '../../services/api.service';
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '../../components/ui/Card';
import {Input} from '../../components/ui/Input';
import {Button} from '../../components/ui/Button';
import {hexToHSL, formatIsoDateToBr} from '../../lib/utils'; // <--- Função de data importada
import {CheckCircle2, Loader2} from 'lucide-react';
import type {FormField, PatientInfo} from '../../types/api';
import * as React from "react";

export default function PublicFormPage() {
    const {uuid} = useParams<{ uuid: string }>();
    const [formAnswers, setFormAnswers] = useState<Record<string, any>>({});
    const [patientInfo, setPatientInfo] = useState<PatientInfo>({
        name: '',
        cpf: '',
        sexo: 'M',
        nascimento: '',
        email: '',
        celular: '',
    });
    const [submitted, setSubmitted] = useState(false);

    // Busca o template do formulário na API
    const {data: template, isLoading} = useQuery({
        queryKey: ['formTemplate', uuid],
        queryFn: () => publicFormsApi.getFormTemplate(uuid!),
        enabled: !!uuid,
    });

    // Aplica a tematização dinâmica (Cor primária da clínica)
    useEffect(() => {
        if (template?.clinicBranding?.primaryColor) {
            const hsl = hexToHSL(template.clinicBranding.primaryColor);
            document.documentElement.style.setProperty('--primary', hsl);
        }
    }, [template]);

    // Realiza o parse seguro do schema JSON (garante que seja um array)
    const formFields: FormField[] = template?.schemaJson
        ? (JSON.parse(template.schemaJson))
        : [];

    // Mutação para envio do formulário
    const submitMutation = useMutation({
        mutationFn: () => publicFormsApi.submitForm(uuid!, {
            patient: {
                ...patientInfo,
                // AQUI ESTÁ A CORREÇÃO: Formata a data para dd/MM/yyyy antes de enviar ao Java
                nascimento: formatIsoDateToBr(patientInfo.nascimento),
            },
            answersJson: JSON.stringify(formAnswers),
        }),
        onSuccess: () => {
            setSubmitted(true);
            window.scrollTo({top: 0, behavior: 'smooth'});
        },
        onError: (error: any) => {
            // Tratamento de erro melhorado (Lê o ProblemDetail do Spring Boot)
            const problemDetail = error.response?.data;

            if (problemDetail?.errors) {
                // Se o backend enviou lista de erros por campo (ex: CPF inválido)
                const messages = Object.values(problemDetail.errors).join('\n');
                alert(`Erro de validação:\n${messages}`);
            } else if (problemDetail?.detail) {
                // Se for um erro genérico do backend com mensagem detalhada
                alert(problemDetail.detail);
            } else {
                // Fallback para erros de rede ou desconhecidos
                alert('Erro ao enviar formulário. Verifique a sua conexão.');
            }
        },
    });

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();

        // Validação local básica dos dados do paciente
        if (!patientInfo.name || !patientInfo.cpf || !patientInfo.nascimento) {
            alert('Por favor, preencha todos os campos obrigatórios do paciente.');
            return;
        }

        submitMutation.mutate();
    };

    if (isLoading) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-50 to-gray-100">
                <Loader2 className="h-8 w-8 animate-spin text-primary"/>
            </div>
        );
    }

    if (!template) {
        return (
            <div
                className="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-50 to-gray-100 p-4">
                <Card className="max-w-md w-full">
                    <CardContent className="pt-6">
                        <p className="text-center text-muted-foreground">Formulário não encontrado.</p>
                    </CardContent>
                </Card>
            </div>
        );
    }

    // Tela de Sucesso
    if (submitted) {
        return (
            <div
                className="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-50 to-gray-100 p-4">
                <Card className="max-w-md w-full">
                    <CardContent className="pt-6 text-center space-y-4">
                        <CheckCircle2 className="h-16 w-16 text-green-500 mx-auto"/>
                        <h2 className="text-2xl font-bold">Formulário Enviado!</h2>
                        <p className="text-muted-foreground">
                            Obrigado por preencher o formulário. Os seus dados foram recebidos com sucesso.
                        </p>
                    </CardContent>
                </Card>
            </div>
        );
    }

    // Formulário Principal
    return (
        <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100">
            {/* Cabeçalho com Branding da Clínica */}
            <header className="bg-primary text-primary-foreground py-6 shadow-md">
                <div className="container mx-auto px-4">
                    <div className="flex items-center space-x-4">
                        {template.clinicBranding.logoUrl && (
                            <img
                                src={template.clinicBranding.logoUrl}
                                alt={template.clinicBranding.name}
                                className="h-12 w-auto bg-white/10 rounded p-1"
                            />
                        )}
                        <div>
                            <h1 className="text-2xl font-bold">{template.clinicBranding.name}</h1>
                            {template.clinicBranding.address && (
                                <p className="text-sm opacity-90">{template.clinicBranding.address}</p>
                            )}
                        </div>
                    </div>
                </div>
            </header>

            {/* Conteúdo do Formulário */}
            <main className="container mx-auto px-4 py-8 max-w-2xl">
                <Card>
                    <CardHeader>
                        <CardTitle>{template.title}</CardTitle>
                        {template.description && (
                            <CardDescription>{template.description}</CardDescription>
                        )}
                    </CardHeader>
                    <CardContent>
                        <form onSubmit={handleSubmit} className="space-y-6">
                            {/* Secção de Informações do Paciente */}
                            <div className="space-y-4">
                                <h3 className="text-lg font-semibold border-b pb-2">Dados do Paciente</h3>

                                <div className="space-y-2">
                                    <label className="text-sm font-medium">Nome Completo *</label>
                                    <Input
                                        value={patientInfo.name}
                                        onChange={(e) => setPatientInfo({...patientInfo, name: e.target.value})}
                                        required
                                    />
                                </div>

                                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                    <div className="space-y-2">
                                        <label className="text-sm font-medium">CPF *</label>
                                        <Input
                                            value={patientInfo.cpf}
                                            onChange={(e) => setPatientInfo({...patientInfo, cpf: e.target.value})}
                                            placeholder="00000000000"
                                            maxLength={11}
                                            required
                                        />
                                    </div>

                                    <div className="space-y-2">
                                        <label className="text-sm font-medium">Data de Nascimento *</label>
                                        <Input
                                            type="date"
                                            value={patientInfo.nascimento}
                                            onChange={(e) => setPatientInfo({
                                                ...patientInfo,
                                                nascimento: e.target.value
                                            })}
                                            required
                                        />
                                    </div>
                                </div>

                                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                    <div className="space-y-2">
                                        <label className="text-sm font-medium">Sexo *</label>
                                        <select
                                            className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                                            value={patientInfo.sexo}
                                            onChange={(e) => setPatientInfo({
                                                ...patientInfo,
                                                sexo: e.target.value as 'M' | 'F'
                                            })}
                                            required
                                        >
                                            <option value="M">Masculino</option>
                                            <option value="F">Feminino</option>
                                        </select>
                                    </div>

                                    <div className="space-y-2">
                                        <label className="text-sm font-medium">Telemóvel/Celular</label>
                                        <Input
                                            value={patientInfo.celular}
                                            onChange={(e) => setPatientInfo({...patientInfo, celular: e.target.value})}
                                            placeholder="(00) 00000-0000"
                                        />
                                    </div>
                                </div>

                                <div className="space-y-2">
                                    <label className="text-sm font-medium">Email</label>
                                    <Input
                                        type="email"
                                        value={patientInfo.email}
                                        onChange={(e) => setPatientInfo({...patientInfo, email: e.target.value})}
                                        placeholder="seu@email.com"
                                    />
                                </div>
                            </div>

                            {/* Campos Dinâmicos do Formulário */}
                            {formFields.length > 0 && (
                                <div className="space-y-4">
                                    <h3 className="text-lg font-semibold border-b pb-2 mt-6">Questionário</h3>

                                    {formFields.map((field) => (
                                        <div key={field.id} className="space-y-2">
                                            <label className="text-sm font-medium">
                                                {field.label} {field.required && '*'}
                                            </label>

                                            {field.type === 'textarea' ? (
                                                <textarea
                                                    className="flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                                                    placeholder={field.placeholder}
                                                    value={formAnswers[field.id] || ''}
                                                    onChange={(e) => setFormAnswers({
                                                        ...formAnswers,
                                                        [field.id]: e.target.value
                                                    })}
                                                    required={field.required}
                                                />
                                            ) : field.type === 'select' ? (
                                                <select
                                                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                                                    value={formAnswers[field.id] || ''}
                                                    onChange={(e) => setFormAnswers({
                                                        ...formAnswers,
                                                        [field.id]: e.target.value
                                                    })}
                                                    required={field.required}
                                                >
                                                    <option value="">Selecione...</option>
                                                    {field.options?.map((option) => (
                                                        <option key={option} value={option}>{option}</option>
                                                    ))}
                                                </select>
                                            ) : (
                                                <Input
                                                    type={field.type}
                                                    placeholder={field.placeholder}
                                                    value={formAnswers[field.id] || ''}
                                                    onChange={(e) => setFormAnswers({
                                                        ...formAnswers,
                                                        [field.id]: e.target.value
                                                    })}
                                                    required={field.required}
                                                />
                                            )}
                                        </div>
                                    ))}
                                </div>
                            )}

                            {/* Botão de Envio */}
                            <Button
                                type="submit"
                                className="w-full mt-6"
                                disabled={submitMutation.isPending}
                            >
                                {submitMutation.isPending ? (
                                    <>
                                        <Loader2 className="mr-2 h-4 w-4 animate-spin"/>
                                        Enviando...
                                    </>
                                ) : (
                                    'Enviar Formulário'
                                )}
                            </Button>
                        </form>
                    </CardContent>
                </Card>
            </main>
        </div>
    );
}