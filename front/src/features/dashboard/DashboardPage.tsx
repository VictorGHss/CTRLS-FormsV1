import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { submissionsApi } from '../../services/api.service';
import { authStorage } from '../../lib/axios';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../../components/ui/Card';
import { formatDateTimeBR } from '../../lib/utils';
import { LogOut, ChevronLeft, ChevronRight, FileText, User } from 'lucide-react';
import type { SubmissionStatus } from '../../types/api';

// Status badge configuration
const getStatusBadge = (status: SubmissionStatus) => {
  const config = {
    PENDING: { variant: 'warning' as const, label: 'Pendente' },
    PROCESSED: { variant: 'success' as const, label: 'Processado' },
    ERROR: { variant: 'destructive' as const, label: 'Erro' },
    SYNC_ERROR: { variant: 'outline' as const, label: 'Erro de Sync' },
  };

  return config[status] || { variant: 'default' as const, label: status };
};

export default function DashboardPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const pageSize = 10;

  const { data, isLoading, error } = useQuery({
    queryKey: ['submissions', page],
    queryFn: () => submissionsApi.getSubmissions({ page, size: pageSize }),
  });

  const handleLogout = () => {
    authStorage.clearAuth();
    navigate('/login');
  };

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="border-b">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold">CTRLS-Forms Dashboard</h1>
            <p className="text-sm text-muted-foreground">Gestão de Submissões</p>
          </div>
          <Button variant="outline" onClick={handleLogout}>
            <LogOut className="mr-2 h-4 w-4" />
            Sair
          </Button>
        </div>
      </header>

      {/* Main Content */}
      <main className="container mx-auto px-4 py-8">
        <Card>
          <CardHeader>
            <CardTitle>Submissões de Formulários</CardTitle>
            <CardDescription>
              Visualize e gerencie todas as submissões recebidas
            </CardDescription>
          </CardHeader>
          <CardContent>
            {/* Loading State */}
            {isLoading && (
              <div className="text-center py-8">
                <p className="text-muted-foreground">Carregando submissões...</p>
              </div>
            )}

            {/* Error State */}
            {error && (
              <div className="text-center py-8">
                <p className="text-destructive">
                  Erro ao carregar submissões. Tente novamente.
                </p>
              </div>
            )}

            {/* Data Table */}
            {data && (
              <>
                <div className="rounded-md border">
                  <table className="w-full">
                    <thead>
                      <tr className="border-b bg-muted/50">
                        <th className="h-12 px-4 text-left align-middle font-medium text-muted-foreground">
                          <div className="flex items-center">
                            <User className="mr-2 h-4 w-4" />
                            Paciente
                          </div>
                        </th>
                        <th className="h-12 px-4 text-left align-middle font-medium text-muted-foreground hidden md:table-cell">
                          <div className="flex items-center">
                            <FileText className="mr-2 h-4 w-4" />
                            Formulário
                          </div>
                        </th>
                        <th className="h-12 px-4 text-left align-middle font-medium text-muted-foreground hidden lg:table-cell">
                          Data
                        </th>
                        <th className="h-12 px-4 text-left align-middle font-medium text-muted-foreground">
                          Status
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {data.content.length === 0 ? (
                        <tr>
                          <td colSpan={4} className="h-24 text-center text-muted-foreground">
                            Nenhuma submissão encontrada
                          </td>
                        </tr>
                      ) : (
                        data.content.map((submission) => {
                          const statusConfig = getStatusBadge(submission.status);
                          return (
                            <tr key={submission.id} className="border-b transition-colors hover:bg-muted/50">
                              <td className="p-4 align-middle font-medium">
                                {submission.patientName}
                              </td>
                              <td className="p-4 align-middle hidden md:table-cell">
                                {submission.formTitle}
                              </td>
                              <td className="p-4 align-middle text-sm text-muted-foreground hidden lg:table-cell">
                                {formatDateTimeBR(submission.createdAt)}
                              </td>
                              <td className="p-4 align-middle">
                                <Badge variant={statusConfig.variant}>
                                  {statusConfig.label}
                                </Badge>
                              </td>
                            </tr>
                          );
                        })
                      )}
                    </tbody>
                  </table>
                </div>

                {/* Pagination */}
                <div className="flex items-center justify-between px-2 py-4">
                  <div className="text-sm text-muted-foreground">
                    Página {data.number + 1} de {data.totalPages} ({data.totalElements} total)
                  </div>
                  <div className="flex items-center space-x-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage((p) => Math.max(0, p - 1))}
                      disabled={page === 0}
                    >
                      <ChevronLeft className="h-4 w-4" />
                      Anterior
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage((p) => p + 1)}
                      disabled={page >= data.totalPages - 1}
                    >
                      Próximo
                      <ChevronRight className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </>
            )}
          </CardContent>
        </Card>
      </main>
    </div>
  );
}

